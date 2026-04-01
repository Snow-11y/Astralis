; queries/injections.scm
; Injects foreign language grammars into embed blocks and ext blocks.
; tree-sitter will attempt to parse the body with the named language's grammar.

; embed_block: java { ... }  kotlin { ... }  etc.
((embed_block
   language: (identifier) @injection.language
   (raw_block) @injection.content)
 (#set! injection.include-children))

; ext_block: ext rust { ... }  ext c { ... }  etc.
((ext_block
   language: (identifier) @injection.language
   (raw_block) @injection.content)
 (#set! injection.include-children))

; Inline asm block content
((asm_expr
   (raw_string_literal) @injection.content)
 (#set! injection.language "asm"))

; SQL inside sql"..." string literals (MIRROR DSL example)
((string_literal) @injection.content
 (#match? @injection.content "^sql\"")
 (#set! injection.language "sql"))
