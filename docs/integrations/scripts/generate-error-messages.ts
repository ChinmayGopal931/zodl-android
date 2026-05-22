/**
 * Generates `offramp-lib/.../orchestrator/KnownContractErrorMessages.kt` from
 * `p2pdotme-sdk/src/contracts/error-messages.ts`. Maps each canonical error code
 * (the SCREAMING_SNAKE constant emitted by generate-revert-selectors.ts) to the
 * SDK's human-readable English string.
 *
 * This is the long-tail fallback copy: the curated PAY-flow reverts render
 * localised `R.string.*` text in the UI, but the ~175 errors we don't curate show
 * this English message instead of a raw `ORDER_NOT_PLACED`-style code. Keep the two
 * generators in lock-step — every code in KnownContractErrors should have a message.
 *
 *   bun /path/to/zodl-android/docs/integrations/scripts/generate-error-messages.ts \
 *     /path/to/p2pdotme-sdk \
 *     > /path/to/zodl-android/offramp-lib/src/jvmMain/kotlin/xyz/justzappit/offramp/orchestrator/KnownContractErrorMessages.kt
 */
import { readFileSync } from "node:fs";

const DEFAULT_PATH = "/Users/chinmaygopal/dev/p2pdotme-sdk";
const sdkPath = process.argv[2] ?? DEFAULT_PATH;
const messagesTsPath = `${sdkPath}/src/contracts/error-messages.ts`;

const src = readFileSync(messagesTsPath, "utf8");

// Parse `contractErrorMessages` — maps SCREAMING_SNAKE code → English string.
// Entries look like:   NOT_ADMIN: "You are not an admin",
// Some span two lines:  USER_HAS_NO_REPUTATION:\n    "Kindly do ...",
const block = src.match(/export const contractErrorMessages[^{]*{([\s\S]*?)\n};/);
if (!block) {
	throw new Error(`Could not locate contractErrorMessages block in ${messagesTsPath}`);
}

// `\s*` after the colon also swallows the newline used for wrapped values; the
// value group tolerates escaped chars but the SDK copy uses none today.
const entryRegex = /^\s*([A-Z][A-Z0-9_]*)\s*:\s*"((?:[^"\\]|\\.)*)",?\s*$/gm;
const codeToMessage = new Map<string, string>();
{
	const body = block[1];
	let m: RegExpExecArray | null;
	while ((m = entryRegex.exec(body))) {
		const [, code, message] = m;
		codeToMessage.set(code, message);
	}
}

if (codeToMessage.size === 0) {
	throw new Error(`Parsed 0 messages from ${messagesTsPath} — regex needs updating`);
}

// Escape for a Kotlin double-quoted string literal.
const esc = (s: string) => s.replace(/\\/g, "\\\\").replace(/"/g, '\\"').replace(/\$/g, "\\$");

const stamp = new Date().toISOString().slice(0, 10);
const sorted = [...codeToMessage.entries()].sort(([a], [b]) => a.localeCompare(b));

const lines: string[] = [];
lines.push("// GENERATED FILE — DO NOT EDIT.");
lines.push("// Source: p2pdotme-sdk/src/contracts/error-messages.ts (contractErrorMessages).");
lines.push(`// Regenerate via docs/integrations/scripts/generate-error-messages.ts (run on ${stamp}).`);
lines.push("//");
lines.push(`// Message count: ${codeToMessage.size}`);
lines.push("");
lines.push("package xyz.justzappit.offramp.orchestrator");
lines.push("");
lines.push("/**");
lines.push(" * English fallback copy for every p2p.me contract error code, mirroring the SDK's");
lines.push(" * `contractErrorMessages`. Keyed by the canonical code from [KnownContractErrors].");
lines.push(" *");
lines.push(" * This is the diagnostic long-tail only: curated PAY-flow reverts render localised");
lines.push(" * `R.string.*` copy in the UI ([KnownRevertReason]). These strings are intentionally not");
lines.push(" * localised — they cover the errors the offramp flow does not raise in normal operation.");
lines.push(" */");
lines.push("object KnownContractErrorMessages {");
lines.push("    private val MESSAGES: Map<String, String> = mapOf(");
for (const [code, message] of sorted) {
	lines.push(`        "${code}" to "${esc(message)}",`);
}
lines.push("    )");
lines.push("");
lines.push("    /** English message for a canonical error [code] from [KnownContractErrors], or null. */");
lines.push("    fun messageFor(code: String?): String? = code?.let { MESSAGES[it] }");
lines.push("");
lines.push("    /** Total count of mapped messages. Exposed for SDK-parity tests. */");
lines.push("    val size: Int get() = MESSAGES.size");
lines.push("}");
lines.push("");

process.stdout.write(lines.join("\n"));
