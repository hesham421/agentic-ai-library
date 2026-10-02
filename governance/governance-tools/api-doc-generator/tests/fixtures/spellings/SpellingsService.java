package com.erp.fixture.service;

/** Every @PreAuthorize/@Secured spelling that has broken permission extraction once. */
public class SpellingsService {

    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants).PERM_FX_SINGLE_LINE)")
    public ServiceResult<Void> singleLine() { return null; }

    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FX_TWO_PARTS)")
    public ServiceResult<Void> twoParts() { return null; }

    @Operation(
        summary = "has (parens) in it",
        description = "and spans lines"
    )
    @PreAuthorize(
        "hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FX_THREE_LINES)")
    public ServiceResult<Void> threeLines() { return null; }

    @PreAuthorize("hasAuthority(T(P)" +
        ".PERM_FX_TRAILING_PLUS)" +
        "")
    public ServiceResult<Void> trailingPlus() { return null; }

    /** No PERM_ prefix at all -- CU spells its constants this way. */
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".CONFIG_CREATE)")
    public ServiceResult<Void> noPermPrefix() { return null; }

    @PreAuthorize("isAuthenticated()")
    public ServiceResult<Void> authenticatedOnly() { return null; }

    @PreAuthorize(value = "hasRole('ADMIN')")
    public ServiceResult<Void> valueAttribute() { return null; }

    @Secured({"ROLE_ADMIN", "ROLE_OPS"})
    public ServiceResult<Void> securedArray() { return null; }

    private static final String ALSO_NOT_AN_ANNOTATION =
        "@PreAuthorize(\"hasAuthority('PERM_FX_IN_A_STRING')\")";

    public ServiceResult<Void> undeclared() { return null; }
}
