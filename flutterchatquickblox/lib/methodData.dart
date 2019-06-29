class MethodData {
  dynamic dataPassed;
  String methodName;

  MethodData(this.methodName, this.dataPassed);

  dynamic get getDataPassed {
    return dataPassed;
  }

  String get getMethodName {
    return methodName;
  }
}
