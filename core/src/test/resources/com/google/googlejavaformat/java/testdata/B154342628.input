class B154342628 {
  void f() {
    var var = 42;
    return writtenVariables.stream()
        .filter(var -> deletedVariableIds.contains(var.getId()))
        .collect(toImmutableList());
  }
}
