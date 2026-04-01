// ❄️ tree-sitter-snowflake — grammar.js
// Full tree-sitter grammar for the Snowflake programming language.
// Covers v4/v5 syntax including all 10 core design features.

module.exports = grammar({
  name: "snowflake",

  extras: $ => [
    /\s/,
    $.comment_line,
    $.comment_block,
    $.doc_block,
  ],

  conflicts: $ => [
    [$.binary_expression, $.unary_expression],
    [$.call_expression, $.index_expression],
    [$.type_ref, $.identifier],
  ],

  word: $ => $.identifier,

  rules: {

    // ─────────────────────────────────────────────────────────────────────────
    // Source file
    // ─────────────────────────────────────────────────────────────────────────

    source_file: $ => repeat($._top_level),

    _top_level: $ => choice(
      $.import_decl,
      $.export_decl,
      $.fn_decl,
      $.class_decl,
      $.struct_decl,
      $.interface_decl,
      $.object_decl,
      $.error_decl,
      $.type_alias_decl,
      $.refinement_type_decl,
      $.template_decl,
      $.extend_decl,
      $.extern_block,
      $.embed_block,
      $.ext_block,
      $.mixin_decl,
      $.code_thread_decl,
      $.string_decl,
      $.lang_thread_decl,
      $.proc_decl,
      $.supervise_decl,
      $.eff_decl,
      $.cap_decl,
      $.arena_decl,
      $.let_decl,
      $.expression_statement,
      $.doc_block,
    ),

    // ─────────────────────────────────────────────────────────────────────────
    // Comments
    // ─────────────────────────────────────────────────────────────────────────

    comment_line:  $ => token(seq('//', /.*/)),
    comment_block: $ => token(seq('/*', /[^*]*\*+([^/*][^*]*\*+)*/, '/')),
    doc_block:     $ => token(seq('!"', /[^]*?/, '!"')),

    // ─────────────────────────────────────────────────────────────────────────
    // Imports & exports
    // ─────────────────────────────────────────────────────────────────────────

    import_decl: $ => choice(
      seq(choice('imp', 'import'), $.module_path),
      seq(choice('get', 'from'), $.module_path, optional(seq('get', $.import_list))),
      seq('from', $.module_path, 'get', $.import_list),
      seq(choice('use', 'bring'), $.module_path, optional(seq('.', choice('*', 'all')))),
    ),

    export_decl: $ => seq(choice('exp', 'export'), $._top_level),

    module_path: $ => seq(
      $.identifier,
      repeat(seq('.', $.identifier)),
    ),

    import_list: $ => seq(
      $.import_item,
      repeat(seq(',', $.import_item)),
    ),

    import_item: $ => seq(
      choice($.identifier, $.type_identifier),
      optional(seq('as', choice($.identifier, $.type_identifier))),
    ),

    // ─────────────────────────────────────────────────────────────────────────
    // Declarations
    // ─────────────────────────────────────────────────────────────────────────

    fn_decl: $ => seq(
      repeat($.annotation),
      repeat($.fn_modifier),
      choice('fn', 'afn', 'ifn', 'cfn', 'ctfn', 'logic', 'rank', 'memo', 'flow'),
      $.identifier,
      optional($.type_params),
      $.param_list,
      optional(seq('->', $.type_expr)),
      optional($.where_clause),
      choice($.block, seq('=', $._expr)),
    ),

    fn_modifier: $ => choice(
      'async', 'inline', 'const', 'pure', 'override', 'ovr',
      'abstract', 'abs', 'private', 'prv', 'public',
      'unsafe', 'usk', 'extern', 'det', 'commute', 'idempotent',
      'rank', 'race_free',
    ),

    class_decl: $ => seq(
      repeat($.annotation),
      optional($.access_modifier),
      choice('class', 'cls', 'data', 'rec', 'sealed', 'seal', 'value', 'vcl', 'abstract', 'abs'),
      $.type_identifier,
      optional($.type_params),
      optional($.primary_ctor),
      optional(seq(':', $.super_types)),
      optional($.where_clause),
      $.class_body,
    ),

    struct_decl: $ => seq(
      repeat($.annotation),
      choice('struct', 'stc'),
      $.type_identifier,
      optional($.type_params),
      optional($.primary_ctor),
      optional($.where_clause),
      $.class_body,
    ),

    interface_decl: $ => seq(
      repeat($.annotation),
      choice('interface', 'ifc'),
      $.type_identifier,
      optional($.type_params),
      optional(seq(':', $.super_types)),
      $.class_body,
    ),

    object_decl: $ => seq(
      repeat($.annotation),
      choice('object', 'obj'),
      $.type_identifier,
      optional(seq(':', $.super_types)),
      $.class_body,
    ),

    error_decl: $ => seq(
      choice('error', 'err'),
      $.type_identifier,
      optional($.primary_ctor),
    ),

    type_alias_decl: $ => seq(
      choice('typealias', 'alias'),
      $.type_identifier,
      optional($.type_params),
      '=',
      $.type_expr,
    ),

    refinement_type_decl: $ => seq(
      'type',
      $.type_identifier,
      '=',
      $.type_expr,
      'where',
      $._expr,
    ),

    template_decl: $ => seq(
      choice('template', 'gen'),
      optional($.type_params),
      choice($.class_decl, $.struct_decl, $.fn_decl),
    ),

    extend_decl: $ => seq(
      choice('extend', 'ext'),
      $.type_identifier,
      optional($.type_params),
      optional($.where_clause),
      '{',
      repeat($.fn_decl),
      '}',
    ),

    extern_block: $ => seq(
      'extern',
      '{',
      repeat($.extern_item),
      '}',
    ),

    extern_item: $ => choice(
      seq('fn', $.identifier, $.param_list, optional(seq('->', $.type_expr))),
      seq(optional('mut'), $.type_expr, $.identifier),
    ),

    embed_block: $ => seq(
      field('language', $.identifier),
      field('body', $.raw_block),
    ),

    ext_block: $ => seq(
      'ext',
      field('language', $.identifier),
      field('body', $.raw_block),
    ),

    raw_block: $ => seq('{', /[^}]*/, '}'),

    // ─────────────────────────────────────────────────────────────────────────
    // Mixin
    // ─────────────────────────────────────────────────────────────────────────

    mixin_decl: $ => seq(
      choice('mixin', 'mix'),
      optional('pseudo', 'psd'),
      $.type_identifier,
      optional(seq('for', repeat1(seq($.type_identifier, optional(','))))),
      '{',
      repeat($.mixin_member),
      '}',
    ),

    mixin_member: $ => choice(
      seq(choice('inject', 'inj'),
          optional(seq('(', 'at', ':', $.identifier, ')')),
          $.fn_decl),
      seq(choice('redirect', 'rdr'), $.fn_decl),
      seq(choice('shadow', 'shd'), $.identifier, ':', $.type_expr),
      seq(choice('overwrite', 'owt'), $.fn_decl),
    ),

    // ─────────────────────────────────────────────────────────────────────────
    // Code Threads & Strings
    // ─────────────────────────────────────────────────────────────────────────

    code_thread_decl: $ => seq(
      choice('thread', 'pub', 'closed', 'prv', 'th', 'pth', 'cth', 'xth'),
      $.type_identifier,
      repeat($.thread_option),
      $.block,
    ),

    thread_option: $ => seq(
      '|',
      choice(
        seq('delay',        ':', $.delay_spec),
        seq('lifetime',     ':', $.lifetime_spec),
        seq('loop',         ':', optional($.duration)),
        seq('extend',       ':', $.duration),
        seq('relationship', ':', $.relationship_spec),
        seq('type',         ':', choice('static', 'adaptive')),
        seq('for',          ':', choice('all', $.identifier)),
        seq('stack',        'count', ':', $.stack_count_spec),
      ),
    ),

    delay_spec: $ => choice(
      seq(choice('before', 'after'), $.identifier),
      $.duration,
    ),

    lifetime_spec: $ => choice(
      'short', 'mid', 'long', 'loop',
      seq('custom', '(', $.duration, ')'),
    ),

    duration: $ => seq(/\d+/, choice('ms', 's', 'min', 'h')),

    relationship_spec: $ => seq(
      choice('devoted', /\d+/),
      'with',
      $.identifier,
      repeat(seq(',', choice('devoted', /\d+/), 'with', $.identifier)),
    ),

    stack_count_spec: $ => choice(
      'auto',
      seq(/\d+/, optional('x')),
    ),

    string_decl: $ => seq(
      optional(choice('lightweight', 'public', 'private', 'closed', 'heavy')),
      'string',
      $.type_identifier,
      repeat($.string_option),
      $.block,
    ),

    string_option: $ => seq(
      '|',
      choice(
        seq('string', 'type', ':', $.concurrency_spec),
        seq('max', 'String', 'size', ':', $.byte_size),
        seq('stack', 'size',         ':', $.byte_size),
        seq('delay',                 ':', $.delay_spec),
        seq('lifetime',              ':', $.lifetime_spec),
        seq('type',                  ':', choice('static', 'adaptive')),
        seq('for',                   ':', choice('all', $.identifier)),
        seq('relationship',          ':', $.relationship_spec),
      ),
    ),

    concurrency_spec: $ => choice(
      'singular', 'duo', 'squad', 'team',
      seq('custom', '(', /\d+/, ')'),
    ),

    byte_size: $ => seq(/\d+/, choice('b', 'kb', 'mb', 'gb')),

    // ─────────────────────────────────────────────────────────────────────────
    // Universal Language Threads
    // ─────────────────────────────────────────────────────────────────────────

    lang_thread_decl: $ => seq(
      'lang',
      field('language', $.identifier),
      'as',
      field('name', $.type_identifier),
      repeat($.lang_thread_option),
      field('body', $.raw_block),
    ),

    lang_thread_option: $ => seq(
      '|',
      choice(
        seq('lifetime',     ':', $.lifetime_spec),
        seq('relationship', ':', $.relationship_spec),
        seq('type',         ':', choice('static', 'adaptive')),
        seq('for',          ':', choice('all', $.identifier)),
        seq('runtime',      ':', choice('detect', $.string_literal)),
        seq('version',      ':', $.string_literal),
      ),
    ),

    // ─────────────────────────────────────────────────────────────────────────
    // STORM — Processes
    // ─────────────────────────────────────────────────────────────────────────

    proc_decl: $ => seq(
      'proc',
      $.type_identifier,
      '{',
      repeat($.proc_member),
      '}',
    ),

    proc_member: $ => choice(
      seq(repeat($.annotation), $.fn_decl),
      $.mailbox_block,
      $.let_decl,
    ),

    mailbox_block: $ => seq(
      'mailbox',
      '{',
      repeat($.mailbox_arm),
      '}',
    ),

    mailbox_arm: $ => seq(
      $._pattern,
      '=>',
      choice($.block, $._expr),
    ),

    supervise_decl: $ => seq(
      'supervise',
      $.type_identifier,
      '(',
      'strategy', ':', $.identifier,
      ')',
      '{',
      repeat($.child_spec),
      '}',
    ),

    child_spec: $ => seq(
      'child', $.identifier,
      optional(seq('restart',      ':', $.identifier)),
      optional(seq('max_restarts', ':', /\d+/)),
      optional(seq('within',       ':', $.duration)),
    ),

    // ─────────────────────────────────────────────────────────────────────────
    // ETHER — Algebraic Effects
    // ─────────────────────────────────────────────────────────────────────────

    eff_decl: $ => seq(
      'eff',
      $.type_identifier,
      optional($.type_params),
      '{',
      repeat($.fn_decl),
      '}',
    ),

    // ─────────────────────────────────────────────────────────────────────────
    // VAULT — Capabilities
    // ─────────────────────────────────────────────────────────────────────────

    cap_decl: $ => seq(
      'cap',
      $.type_identifier,
      optional(choice(
        seq(':', $.type_identifier),
        seq('=', $.type_identifier, repeat(seq('+', $.type_identifier))),
      )),
    ),

    // ─────────────────────────────────────────────────────────────────────────
    // ARENA — Share-Nothing Arenas
    // ─────────────────────────────────────────────────────────────────────────

    arena_decl: $ => seq(
      'arena',
      $.type_identifier,
      optional(seq(':', $.byte_size)),
      $.block,
    ),

    // ─────────────────────────────────────────────────────────────────────────
    // Variables
    // ─────────────────────────────────────────────────────────────────────────

    let_decl: $ => choice(
      seq($.identifier, ':=', $._expr),
      seq($.identifier, '::=', $._expr),
      seq('fix', $.identifier, optional(seq(':', $.type_expr)), '=', $._expr),
      seq('let', $.identifier, optional(seq(':', $.type_expr)), '=', $._expr),
      seq('var', $.identifier, optional(seq(':', $.type_expr)), '=', $._expr),
      seq('val', $.identifier, optional(seq(':', $.type_expr)), '=', $._expr),
      seq('lazy', $.identifier, optional(seq(':', $.type_expr)), '=', $._expr),
      seq('thunk', $.identifier, optional(seq(':', $.type_expr)), '=', $._expr),
    ),

    // ─────────────────────────────────────────────────────────────────────────
    // Statements
    // ─────────────────────────────────────────────────────────────────────────

    expression_statement: $ => $._expr,

    for_statement: $ => seq(
      'for',
      $.identifier,
      optional(seq(',', $.identifier)),
      'in',
      $._expr,
      $.block,
    ),

    while_statement: $ => seq(
      choice('while', 'whl'),
      $._expr,
      $.block,
    ),

    until_statement: $ => seq(
      choice('until', 'utl'),
      $._expr,
      $.block,
    ),

    loop_statement: $ => seq('loop', $.block),

    // ─────────────────────────────────────────────────────────────────────────
    // Expressions
    // ─────────────────────────────────────────────────────────────────────────

    _expr: $ => choice(
      $.integer_literal,
      $.float_literal,
      $.bool_literal,
      $.null_literal,
      $.string_literal,
      $.raw_string_literal,
      $.byte_string_literal,
      $.identifier,
      $.self_expr,
      $.super_expr,
      $.it_expr,
      $.list_literal,
      $.set_literal,
      $.map_literal,
      $.named_tuple_literal,
      $.block,
      $.if_expr,
      $.unless_expr,
      $.when_expr,
      $.try_expr,
      $.lambda_expr,
      $.binary_expression,
      $.unary_expression,
      $.field_access,
      $.index_expression,
      $.call_expression,
      $.await_expr,
      $.parallel_expr,
      $.race_expr,
      $.select_expr,
      $.emit_expr,
      $.pipe_expr,
      $.alt_expr,
      $.compose_expr,
      $.propagate_expr,
      $.return_expr,
      $.throw_expr,
      $.break_expr,
      $.continue_expr,
      $.do_block,
      $.unsafe_block,
      $.comptime_expr,
      $.asm_expr,
      $.spawn_expr,
      $.range_expr,
      $.assign_expr,
      $.is_expr,
      $.as_expr,
      $.dbg_expr,
      $.quote_expr,
      $.perform_expr,
      $.handle_expr,
      $.det_block,
      $.fork_join_block,
      $.solve_expr,
    ),

    integer_literal: $ => token(seq(
      choice(
        /0[xX][0-9a-fA-F_]+/,
        /0[bB][01_]+/,
        /0[oO][0-7_]+/,
        /\d[\d_]*/,
      ),
      optional(/[lLuUiI](?:8|16|32|64)?|usize|isize/),
    )),

    float_literal: $ => token(seq(
      /\d[\d_]*\.\d[\d_]*/,
      optional(/[eE][+-]?\d+/),
      optional(/[fFdD]|_f32|_f64/),
    )),

    bool_literal:  $ => choice('true', 'false'),
    null_literal:  $ => 'null',
    self_expr:     $ => 'this',
    super_expr:    $ => 'super',
    it_expr:       $ => 'it',

    string_literal: $ => seq(
      '"',
      repeat(choice(
        /[^"$\\]+/,
        $.string_escape,
        $.interp_simple,
        $.interp_block,
      )),
      '"',
    ),

    raw_string_literal:  $ => seq('r"', /[^"]*/, '"'),
    byte_string_literal: $ => seq('b"', repeat(choice(/[^"\\]/, $.string_escape)), '"'),

    string_escape: $ => /\\[ntr"'\\$0]/,

    interp_simple: $ => seq('$', $.identifier),
    interp_block:  $ => seq('${', $._expr, '}'),

    list_literal:  $ => seq('[', commaSep($._expr), ']'),
    set_literal:   $ => seq('#{', commaSep($._expr), '}'),
    map_literal:   $ => seq('{', commaSep($.map_entry), '}'),
    map_entry:     $ => seq($._expr, ':', $._expr),

    named_tuple_literal: $ => seq(
      '(',
      commaSep(seq($.identifier, ':', $._expr)),
      ')',
    ),

    block: $ => seq(
      '{',
      repeat(choice(
        $.let_decl,
        $.for_statement,
        $.while_statement,
        $.until_statement,
        $.loop_statement,
        $.fn_decl,
        $.class_decl,
        $.expression_statement,
      )),
      '}',
    ),

    if_expr: $ => seq(
      'if',
      field('condition', $._expr),
      field('consequence', $.block),
      optional(seq('else', field('alternative', choice($.block, $.if_expr)))),
    ),

    unless_expr: $ => seq(
      choice('unless', 'nif'),
      field('condition', $._expr),
      field('consequence', $.block),
      optional(seq('else', field('alternative', $.block))),
    ),

    when_expr: $ => seq(
      'when',
      optional($._expr),
      '{',
      repeat($.when_arm),
      '}',
    ),

    when_arm: $ => seq($._pattern, optional(seq('if', $._expr)), '=>', $._arm_body),
    _arm_body: $ => choice($.block, $._expr),

    try_expr: $ => seq(
      'try',
      $.block,
      repeat($.catch_arm),
      optional(seq(choice('finally', 'fin'), $.block)),
    ),

    catch_arm: $ => seq(
      choice('catch', 'cth'),
      optional(seq($.type_identifier, optional($.identifier))),
      $.block,
    ),

    lambda_expr: $ => choice(
      seq('fn', '(', commaSep($.param), ')', optional(seq('->', $.type_expr)), '=>', $._expr),
      seq('(', commaSep($.param), ')', '=>', $._expr),
      seq($.identifier, '=>', $._expr),
    ),

    binary_expression: $ => choice(
      prec.left(1,  seq($._expr, choice('or',  '||'),           $._expr)),
      prec.left(2,  seq($._expr, choice('and', '&&'),           $._expr)),
      prec.left(3,  seq($._expr, choice('==', '!=', '~='),      $._expr)),
      prec.left(4,  seq($._expr, choice('<', '>', '<=', '>='),  $._expr)),
      prec.left(5,  seq($._expr, '??',                          $._expr)),
      prec.left(6,  seq($._expr, '<>',                          $._expr)),
      prec.left(7,  seq($._expr, choice('+', '-'),              $._expr)),
      prec.left(8,  seq($._expr, choice('*', '/', '%', '%%'),   $._expr)),
      prec.right(9, seq($._expr, '**',                          $._expr)),
      prec.left(10, seq($._expr, '|>',                          $._expr)),
      prec.left(11, seq($._expr, '<|>',                         $._expr)),
      prec.left(12, seq($._expr, choice('>>', '<<'),            $._expr)),
      prec.left(4,  seq($._expr, '=:=',                         $._expr)),
    ),

    unary_expression: $ => prec(13, choice(
      seq('not', $._expr),
      seq('!',   $._expr),
      seq('-',   $._expr),
      seq('move', $._expr),
      seq('own',  $._expr),
      seq('pin',  $._expr),
      seq('leak', $._expr),
      seq('force', $._expr),
      seq('~', $._expr),
    )),

    field_access: $ => prec.left(14, seq(
      $._expr,
      choice('.', '?.'),
      $.identifier,
    )),

    index_expression: $ => prec.left(14, seq($._expr, '[', $._expr, ']')),

    call_expression: $ => prec.left(14, seq(
      $._expr,
      optional($.type_args),
      '(',
      commaSep($.argument),
      ')',
    )),

    argument: $ => seq(optional(seq($.identifier, ':')), $._expr),

    await_expr:    $ => seq(choice('await', 'awt'), $._expr),
    parallel_expr: $ => seq(choice('parallel', 'par'), $.block),
    race_expr:     $ => seq('race', $.block),
    select_expr:   $ => seq(choice('select', 'sel'), '{', repeat($.select_arm), '}'),
    select_arm:    $ => seq($._expr, '=>', $.block),
    emit_expr:     $ => seq(choice('emit', 'emt'), $._expr),

    pipe_expr: $ => prec.left(10, seq($._expr, '|>', $._expr)),
    alt_expr:  $ => prec.left(11, seq($._expr, '<|>', $._expr)),
    compose_expr: $ => prec.left(12, seq($._expr, choice('>>', '<<'), $._expr)),

    propagate_expr: $ => prec(14, seq($._expr, '?')),

    return_expr:   $ => seq(choice('return', 'ret'), optional($._expr)),
    throw_expr:    $ => seq(choice('throw', 'thr'), $._expr),
    break_expr:    $ => seq(choice('break', 'brk'), optional($._expr)),
    continue_expr: $ => choice('continue', 'cnt'),
    do_block:      $ => seq('do', $.block),
    unsafe_block:  $ => seq(choice('unsafe', 'usk'), $.block),
    comptime_expr: $ => seq(choice('comptime', 'ctime'), $.block),
    dbg_expr:      $ => seq('dbg', $._expr),

    asm_expr: $ => seq(
      'asm',
      '{',
      $.raw_string_literal,
      '}',
    ),

    spawn_expr: $ => seq(
      'spawn',
      $.type_identifier,
      optional(seq('(', commaSep($._expr), ')')),
      optional(seq('*', $._expr)),
    ),

    range_expr: $ => prec.left(6, seq(
      $._expr,
      choice('..', '..='),
      $._expr,
    )),

    assign_expr: $ => prec.right(0, seq(
      $._expr,
      choice(':=', '::=', '=', '+=', '-=', '*=', '/=', '%='),
      $._expr,
    )),

    is_expr: $ => seq($._expr, 'is', $.type_identifier, optional($.identifier)),
    as_expr: $ => seq($._expr, 'as', optional('?'), $.type_expr),

    quote_expr: $ => seq('quote', $.block),

    perform_expr: $ => seq(
      'perform',
      $.type_identifier,
      '.',
      $.identifier,
      optional(seq('(', commaSep($._expr), ')')),
    ),

    handle_expr: $ => seq(
      'handle',
      $.block,
      'with',
      '{',
      repeat($.handle_arm),
      '}',
    ),

    handle_arm: $ => choice(
      seq('effect', $.type_identifier, '.', $.identifier,
          optional(seq('(', commaSep($.identifier), ')')),
          '=>', $._arm_body),
      seq('return', $.identifier, '=>', $._arm_body),
      seq('delegate', $.type_identifier),
    ),

    det_block: $ => seq(
      'det',
      optional(seq('schedule', '(', $.identifier, optional(seq(',', 'partitions', ':', /\d+/)), ')')),
      $.block,
    ),

    fork_join_block: $ => seq('fork_join', $.block),

    solve_expr: $ => seq(
      'solve',
      optional(choice('all', /\d+/)),
      $.block,
    ),

    // ─────────────────────────────────────────────────────────────────────────
    // Patterns
    // ─────────────────────────────────────────────────────────────────────────

    _pattern: $ => choice(
      $.wildcard_pattern,
      $.null_pattern,
      $.bool_pattern,
      $.integer_literal,
      $.string_literal,
      $.range_pattern,
      $.type_pattern,
      $.destructure_pattern,
      $.else_pattern,
      $._expr,
    ),

    wildcard_pattern:    $ => '_',
    null_pattern:        $ => 'null',
    bool_pattern:        $ => choice('true', 'false'),
    else_pattern:        $ => 'else',
    range_pattern:       $ => seq($._expr, choice('..', 'in'), $._expr),
    type_pattern:        $ => seq('is', $.type_identifier, optional($.identifier)),
    destructure_pattern: $ => seq(
      'is', $.type_identifier,
      '(', commaSep($.identifier), ')',
    ),

    // ─────────────────────────────────────────────────────────────────────────
    // Types
    // ─────────────────────────────────────────────────────────────────────────

    type_expr: $ => choice(
      $.type_identifier,
      $.generic_type,
      $.nullable_type,
      $.fn_type,
      $.tuple_type,
      $.lazy_type,
      $.borrow_type,
    ),

    generic_type:   $ => seq($.type_identifier, '<', commaSep($.type_expr), '>'),
    nullable_type:  $ => seq($.type_expr, '?'),
    fn_type:        $ => seq('fn', '(', commaSep($.type_expr), ')', '->', $.type_expr),
    tuple_type:     $ => seq('(', commaSep($.type_expr), ')'),
    lazy_type:      $ => seq('~', $.type_expr),
    borrow_type:    $ => seq(choice('&', '&mut'), $.type_expr),

    type_params: $ => seq('<', commaSep($.type_param), '>'),
    type_param:  $ => seq($.type_identifier, optional(seq(':', $.type_expr))),
    type_args:   $ => seq('<', commaSep($.type_expr), '>'),

    where_clause: $ => seq(
      'where',
      commaSep(seq($.type_expr, ':', $.type_expr)),
    ),

    super_types: $ => commaSep1($.type_identifier),

    // ─────────────────────────────────────────────────────────────────────────
    // Parameters, class members
    // ─────────────────────────────────────────────────────────────────────────

    param_list: $ => seq('(', commaSep($.param), ')'),

    param: $ => seq(
      optional('~'),
      optional($.identifier),
      optional(seq($.identifier, ':')),
      optional('?'),
      $.type_expr,
      optional(seq('=', $._expr)),
    ),

    primary_ctor: $ => seq('(', commaSep($.param), ')'),

    class_body: $ => seq('{', repeat($.class_member), '}'),

    class_member: $ => choice(
      $.fn_decl,
      $.let_decl,
      $.init_block,
      $.drop_block,
      $.companion_decl,
    ),

    init_block: $ => seq('init', $.block),
    drop_block: $ => seq('drop', $.block),

    companion_decl: $ => seq(
      choice('companion', 'comp'),
      $.block,
    ),

    // ─────────────────────────────────────────────────────────────────────────
    // Identifiers
    // ─────────────────────────────────────────────────────────────────────────

    type_identifier: $ => /[A-Z][a-zA-Z0-9_]*/,
    identifier:      $ => /[a-z_][a-zA-Z0-9_]*/,

    access_modifier: $ => choice(
      'public', 'private', 'prv', 'abstract', 'abs',
      'sealed', 'open',
    ),

    annotation: $ => seq('@', $.identifier, optional(seq('(', commaSep($._expr), ')'))),
  },
});

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

function commaSep(rule) {
  return optional(commaSep1(rule));
}

function commaSep1(rule) {
  return seq(rule, repeat(seq(',', rule)), optional(','));
}
