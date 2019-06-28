class MethodData {
  dynamic dataPassed;
  String methodName;

  MethodData(methodName, dataPassed);

  dynamic get getDataPassed {
    return dataPassed;
  }

  String get getMethodName {
    return methodName;
  }
}
