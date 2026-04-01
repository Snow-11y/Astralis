; queries/locals.scm — scope and definition tracking

; ─── Scopes ──────────────────────────────────────────────────────────────────
(source_file) @local.scope
(block)       @local.scope
(fn_decl)     @local.scope
(class_body)  @local.scope
(lambda_expr) @local.scope

; ─── Definitions ─────────────────────────────────────────────────────────────
(fn_decl    name: (identifier) @local.definition)
(let_decl   (identifier)       @local.definition)
(param      (identifier)       @local.definition)

(class_decl   (type_identifier) @local.definition)
(struct_decl  (type_identifier) @local.definition)
(proc_decl    (type_identifier) @local.definition)
(eff_decl     (type_identifier) @local.definition)
(cap_decl     (type_identifier) @local.definition)

; ─── References ──────────────────────────────────────────────────────────────
(identifier)      @local.reference
(type_identifier) @local.reference
