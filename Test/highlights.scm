; ❄️ Snowflake tree-sitter highlight queries
; queries/highlights.scm
; Maps parse-tree nodes → @highlight.names that editors use for coloring.

; ─── Comments ────────────────────────────────────────────────────────────────
(comment_line)  @comment
(comment_block) @comment
(doc_block)     @comment.documentation

; ─── Literals ─────────────────────────────────────────────────────────────────
(integer_literal)       @number
(float_literal)         @number.float
(bool_literal)          @constant.builtin
(null_literal)          @constant.builtin
(string_literal)        @string
(raw_string_literal)    @string
(byte_string_literal)   @string.special
(string_escape)         @string.escape
(interp_simple)         @string.special.symbol
(interp_block)          @string.special.symbol

; ─── Keywords: control flow ──────────────────────────────────────────────────
[
  "if" "else" "nif" "unless" "when" "match"
  "for" "while" "whl" "until" "utl" "loop" "do"
  "break" "brk" "continue" "cnt" "return" "ret"
  "try" "catch" "cth" "finally" "fin" "throw" "thr"
] @keyword.control

; ─── Keywords: declarations ──────────────────────────────────────────────────
[
  "fn" "afn" "ifn" "cfn" "ctfn" "flow"
] @keyword.function

[
  "class" "cls" "struct" "stc" "interface" "ifc"
  "data" "rec" "sealed" "seal" "value" "vcl"
  "object" "obj" "companion" "comp"
  "typealias" "alias" "template" "gen"
  "error" "err" "extend"
] @keyword.type

[
  "let" "var" "val" "fix" "mut"
] @keyword.variable

[
  "import" "imp" "get" "from" "use" "bring"
  "export" "exp" "all"
] @keyword.import

[
  "async" "inline" "const" "pure"
  "override" "ovr" "abstract" "abs"
  "private" "prv" "public" "unsafe" "usk"
  "extern" "static" "sealed" "open"
] @keyword.modifier

; ─── Operators ───────────────────────────────────────────────────────────────
[
  "and" "or" "not" "is" "in" "as"
] @keyword.operator

[ ":=" "::=" ] @operator.special    ; walrus — lavender+bold in theme
[ "=>" ]       @operator.fat-arrow
[ "->" ]       @operator.arrow
[ "|>" ]       @operator.pipe
[ "<|>" ]      @operator.alt
[ ">>" "<<" ]  @operator.compose
[ "<>" ]       @operator.concat
[ "**" ]       @operator.power
[ "%%" ]       @operator.pos-modulo
[ "~=" ]       @operator.approx-eq
[ "=:=" ]      @operator.unify
[ "??" ]       @operator.null-coalesce
[ "?" "!" "?." "!!" ] @operator.optional
[ ".." "..=" ]  @operator.range
[ "=" "+=" "-=" "*=" "/=" "%=" ] @operator.assignment
[ "==" "!=" "<" ">" "<=" ">=" ]  @operator.comparison
[ "+" "-" "*" "/" "%" ]          @operator.arithmetic
[ "&" "|" "^" "~" ]              @operator.bitwise

; ─── FROST — ownership ───────────────────────────────────────────────────────
[
  "own" "move" "borrow" "lft" "pin" "leak" "drop_guard"
] @keyword.frost

; ─── FORGE — linear types ────────────────────────────────────────────────────
[
  "lin" "aff" "rel" "ord" "consume" "discard"
  "token" "split" "merge" "with_linear" "check_linear"
] @keyword.forge

; ─── CRYSTAL — comptime ──────────────────────────────────────────────────────
[
  "ctfn" "ctval" "ct_if" "ct_for" "ct_switch"
  "static_assert" "meta" "reflect" "eval_at" "ctime" "comptime"
] @keyword.crystal

; ─── GLACIER — regions ───────────────────────────────────────────────────────
[
  "rgn" "alloc_in" "free_rgn" "pool" "lend_rgn" "bulk_free"
] @keyword.glacier

; ─── STORM — processes ───────────────────────────────────────────────────────
[
  "proc" "mailbox" "supervise" "link" "monitor"
  "heartbeat" "restart" "hot_swap" "escalate"
] @keyword.storm

; ─── PRISM — deterministic parallel ──────────────────────────────────────────
[
  "det" "par_det" "fork_join" "sync_point" "race_free"
  "commute" "idempotent" "quiesce"
] @keyword.prism

; ─── MIRROR — homoiconicity ───────────────────────────────────────────────────
[
  "quote" "unquote" "splice" "macro" "expand"
  "reader" "ast_of" "transform" "syntax"
] @keyword.mirror

; ─── ETHER — effects ─────────────────────────────────────────────────────────
[
  "eff" "perform" "handle" "resume" "abort" "effect" "delegate"
] @keyword.ether

; ─── AXIOM — dependent types ─────────────────────────────────────────────────
[
  "given" "proof" "pi" "sigma" "refl" "forall" "exists" "refined"
] @keyword.axiom

; ─── VAULT — capabilities ────────────────────────────────────────────────────
[
  "cap" "grant" "revoke" "delegate" "restrict" "needs" "seals"
] @keyword.vault

; ─── DEFER — lazy ────────────────────────────────────────────────────────────
[
  "lazy" "thunk" "force" "memo" "defer" "infinite"
] @keyword.defer

; ─── LOGIC — unification ─────────────────────────────────────────────────────
[
  "logic" "clause" "solve" "once" "cut" "fail" "fresh" "unify" "each"
] @keyword.logic

; ─── RANK ────────────────────────────────────────────────────────────────────
[ "rank" "over" "axes" ] @keyword.rank

; ─── ARENA ───────────────────────────────────────────────────────────────────
[
  "arena" "msg" "recv" "request" "broadcast"
  "snapshot" "restore" "boundary" "isolate"
] @keyword.arena

; ─── Concurrency ─────────────────────────────────────────────────────────────
[
  "spawn" "join" "await" "awt" "parallel" "par"
  "race" "select" "sel" "channel" "chan" "emit" "emt" "send"
] @keyword.concurrency

; ─── Mixin ───────────────────────────────────────────────────────────────────
[
  "mixin" "mix" "pseudo" "psd"
  "inject" "inj" "redirect" "rdr"
  "shadow" "shd" "overwrite" "owt"
] @keyword.mixin

; ─── Thread / String declarations ────────────────────────────────────────────
[
  "thread" "pub" "closed" "prv" "th" "pth" "cth" "xth"
  "lang" "runtime" "version" "detect"
] @keyword.thread

[
  "lightweight" "heavy" "string"
  "singular" "duo" "squad" "team" "custom"
] @keyword.string-fiber

; ─── Special words ───────────────────────────────────────────────────────────
[ "this" "super" "it" ] @variable.builtin
[ "true" "false" "null" ] @constant.builtin

; ─── Names ───────────────────────────────────────────────────────────────────
(fn_decl      name: (identifier)      @function.definition)
(class_decl   (type_identifier)       @type.definition)
(struct_decl  (type_identifier)       @type.definition)
(error_decl   (type_identifier)       @type.definition)
(eff_decl     (type_identifier)       @type.definition)
(cap_decl     (type_identifier)       @type.definition)
(proc_decl    (type_identifier)       @type.definition)
(arena_decl   (type_identifier)       @type.definition)

(call_expression
  (field_access field: (identifier) @function.call))

(call_expression
  (identifier) @function.call)

(type_expr     (type_identifier) @type)
(generic_type  (type_identifier) @type)
(type_pattern  (type_identifier) @type)

(lang_thread_decl
  language: (identifier) @string.special.language
  name:     (type_identifier) @type.definition)

(embed_block
  language: (identifier) @string.special.language)

(ext_block
  language: (identifier) @string.special.language)

; ─── Punctuation ─────────────────────────────────────────────────────────────
[ "{" "}" ] @punctuation.bracket
[ "[" "]" ] @punctuation.bracket
[ "(" ")" ] @punctuation.bracket
[ "," ]     @punctuation.delimiter
[ ";" ]     @punctuation.delimiter
[ "." ]     @punctuation.delimiter
[ ":" ]     @punctuation.delimiter
[ "@" ]     @punctuation.special

; ─── Annotations ─────────────────────────────────────────────────────────────
(annotation "@" @punctuation.special)
(annotation (identifier) @attribute)

; ─── Variables ───────────────────────────────────────────────────────────────
(identifier) @variable

; ─── Embedded code — dim ─────────────────────────────────────────────────────
(raw_block) @embedded
