/**
 * Generates `offramp-lib/.../orchestrator/KnownContractErrors.kt` from
 * `user-app-client/src/lib/errors.ts`. Keeps our hex-selector → SDK error-name
 * map byte-aligned with @p2pdotme/user-app-client v3.1.5+.
 *
 * Pure regex parse — no bun/node import-path gymnastics. The source TS file
 * has a stable shape (two object literals), so the regexes below cover all
 * entries today. Re-run whenever the SDK adds new custom errors:
 *
 *   bun /path/to/zodl-android/docs/integrations/scripts/generate-revert-selectors.ts \
 *     /path/to/user-app-client \
 *     > /path/to/zodl-android/offramp-lib/src/jvmMain/kotlin/xyz/justzappit/offramp/orchestrator/KnownContractErrors.kt
 *
 * Diff the committed Kotlin file in your PR — the count of selectors should
 * monotonically grow with each SDK release; a drop signals upstream removed
 * an error you may still want to handle.
 */
import { readFileSync } from "node:fs";

const DEFAULT_PATH = "/Users/chinmaygopal/dev/user-app-client";
const userAppPath = process.argv[2] ?? DEFAULT_PATH;
const errorsTsPath = `${userAppPath}/src/lib/errors.ts`;

const src = readFileSync(errorsTsPath, "utf8");

// 1. Parse `contractErrors` — maps Kotlin-side constant key → snake_string error name.
//    Lines look like:   NotAdmin: "NOT_ADMIN",
//                       BuyOrderAmountExceedsLimit: "BUY_ORDER_AMOUNT_EXCEEDS_LIMIT",
const constToName = new Map<string, string>();
const contractErrorsBlock = src.match(/export const contractErrors\s*=\s*{([\s\S]*?)};/);
if (!contractErrorsBlock) {
	throw new Error(`Could not locate contractErrors block in ${errorsTsPath}`);
}
const constLineRegex = /^\s*([A-Za-z][A-Za-z0-9]*)\s*:\s*"([A-Z0-9_]+)",?\s*$/gm;
{
	const body = contractErrorsBlock[1];
	let m: RegExpExecArray | null;
	while ((m = constLineRegex.exec(body))) {
		const [, key, snakeName] = m;
		constToName.set(key, snakeName);
	}
}

// 2. Parse `hexContractErrors` — selector → contractErrors.<constKey>.
const hexBlock = src.match(/export const hexContractErrors\s*=\s*{([\s\S]*?)};/);
if (!hexBlock) {
	throw new Error(`Could not locate hexContractErrors block in ${errorsTsPath}`);
}
const hexLineRegex =
	/^\s*"(0x[a-fA-F0-9]{8})"\s*:\s*contractErrors\.([A-Za-z][A-Za-z0-9]*),?\s*$/gm;
const selectorToName = new Map<string, string>();
{
	const body = hexBlock[1];
	let m: RegExpExecArray | null;
	while ((m = hexLineRegex.exec(body))) {
		const [, selector, constKey] = m;
		const sdkName = constToName.get(constKey);
		if (!sdkName) {
			throw new Error(
				`hexContractErrors references unknown constant key '${constKey}' for ${selector}`,
			);
		}
		// Lowercase the selector hex for consistency with our Selector4.fromHex output.
		selectorToName.set(selector.toLowerCase(), sdkName);
	}
}

if (selectorToName.size === 0) {
	throw new Error(`Parsed 0 selectors from ${errorsTsPath} — regex needs updating`);
}

// 3. Emit Kotlin. Stable ordering (sorted by selector) so re-runs produce zero diff
//    unless the underlying table actually changed.
const stamp = new Date().toISOString().slice(0, 10);
const sorted = [...selectorToName.entries()].sort(([a], [b]) => a.localeCompare(b));

const lines: string[] = [];
lines.push("// GENERATED FILE — DO NOT EDIT.");
lines.push("// Source: user-app-client/src/lib/errors.ts (hexContractErrors).");
lines.push(`// Regenerate via docs/integrations/scripts/generate-revert-selectors.ts (run on ${stamp}).`);
lines.push("//");
lines.push(`// Selector count: ${selectorToName.size}`);
lines.push("");
lines.push("package xyz.justzappit.offramp.orchestrator");
lines.push("");
lines.push("import xyz.justzappit.evm.abi.Selector4");
lines.push("");
lines.push("/**");
lines.push(" * Wholesale port of the p2p.me Diamond's custom-error selector table. Every selector");
lines.push(" * the contract emits is mapped to its canonical SDK error name (the same string the");
lines.push(" * official `@p2pdotme/user-app-client` displays via i18n keys). Keeps us byte-aligned");
lines.push(" * with the SDK; do not edit by hand. See script header for regeneration.");
lines.push(" */");
lines.push("object KnownContractErrors {");
lines.push("    private val SELECTOR_TO_NAME: Map<Selector4, String> = mapOf(");
for (const [selector, sdkName] of sorted) {
	lines.push(`        Selector4.fromHex("${selector}") to "${sdkName}",`);
}
lines.push("    )");
lines.push("");
lines.push("    /** Returns the SDK error name for [selector], or null if unrecognised. */");
lines.push("    fun nameFor(selector: Selector4?): String? = selector?.let { SELECTOR_TO_NAME[it] }");
lines.push("");
lines.push("    /** Total count of mapped selectors. Exposed for monotonic-growth tests. */");
lines.push("    val size: Int get() = SELECTOR_TO_NAME.size");
lines.push("}");
lines.push("");

process.stdout.write(lines.join("\n"));
