@BugPattern(category = GUAVA, severity = NOT_A_PROBLEM, maturity = EXPERIMENTAL)
@BugPattern(
    name = "AsyncFunctionReturnsImmediate", summary = SIMPLIFY,
    category = GUAVA, severity = NOT_A_PROBLEM, maturity = EXPERIMENTAL)
@BugPattern(
    name = "AsyncFunctionReturnsImmediate", summary = SIMPLIFY,
    explanation =
        "If an AsyncFunction always returns immediateFuture() and never throws, it can "
        + "be replaced with a Function.",
    category = GUAVA, severity = NOT_A_PROBLEM, maturity = EXPERIMENTAL)
class Test {
}
