import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'dart:developer';

class ChatWindow extends StatefulWidget {
  @override
  _ChatWindowState createState() => _ChatWindowState();
}

class _ChatWindowState extends State<ChatWindow> {
  List<String> _messages;

  TextEditingController textEditingController;
  ScrollController scrollController;
  String _batteryPercentage = 'Battery precentage';

  bool enableButton = false;

  @override
  void initState() {
    _messages = List<String>();

    _messages.add("Hi! How are you?");
    // _messages.add("I'm fine. thanks");
    // _messages.add("This is a multiline message.\nKeep reading!");
    // _messages.add("And this is a very..\nvery..\nlong..\nmessage.");
    // _messages.add("Hi! How are you?");
    // _messages.add("I'm fine. thanks");
    // _messages.add("This is a multiline message.\nKeep reading!");
    // _messages.add("And this is a very..\nvery..\nlong..\nmessage.");

    textEditingController = TextEditingController();

    scrollController = ScrollController();

    _getChatHistory();

    super.initState();
  }

  void handleSendMessage() {
    var text = textEditingController.value.text;
    textEditingController.clear();
    _getBatteryInformation(text);

    setState(() {
      _messages.add(text + " " + _batteryPercentage);
      enableButton = false;
    });

    Future.delayed(Duration(milliseconds: 100), () {
      scrollController.animateTo(scrollController.position.maxScrollExtent,
          curve: Curves.ease, duration: Duration(milliseconds: 500));
    });
  }

  @override
  Widget build(BuildContext context) {
    var textInput = Row(
      children: <Widget>[
        Expanded(
          child: Padding(
            padding: const EdgeInsets.only(left: 8.0),
            child: TextField(
              onChanged: (text) {
                setState(() {
                  enableButton = text.isNotEmpty;
                });
              },
              decoration: InputDecoration.collapsed(
                hintText: "Type a message",
              ),
              controller: textEditingController,
            ),
          ),
        ),
        enableButton
            ? IconButton(
                color: Theme.of(context).primaryColor,
                icon: Icon(
                  Icons.send,
                ),
                disabledColor: Colors.grey,
                onPressed: handleSendMessage,
              )
            : IconButton(
                color: Colors.blue,
                icon: Icon(
                  Icons.send,
                ),
                disabledColor: Colors.grey,
                onPressed: null,
              )
      ],
    );

    return Scaffold(
      resizeToAvoidBottomPadding: true,
      appBar: AppBar(
        title: Text("QuickBlox"),
      ),
      body: Column(
        children: <Widget>[
          Expanded(
            child: ListView.builder(
              controller: scrollController,
              itemCount: _messages.length,
              itemBuilder: (context, index) {
                bool reverse = false;

                if (index % 2 == 0) {
                  reverse = true;
                }

                var avatar = Padding(
                  padding:
                      const EdgeInsets.only(left: 8.0, bottom: 8.0, right: 8.0),
                  child: CircleAvatar(
                    child: Text("A"),
                  ),
                );

                var triangle = CustomPaint(
                  painter: Triangle(),
                );

                var messagebody = DecoratedBox(
                  decoration: BoxDecoration(
                    color: Colors.amber,
                    borderRadius: BorderRadius.circular(8.0),
                  ),
                  child: Align(
                    alignment: Alignment.centerLeft,
                    child: Padding(
                      padding: const EdgeInsets.all(12.0),
                      child: Text(_messages[index]),
                    ),
                  ),
                );

                Widget message;

                if (reverse) {
                  message = Stack(
                    children: <Widget>[
                      messagebody,
                      Positioned(right: 0, bottom: 0, child: triangle),
                    ],
                  );
                } else {
                  message = Stack(
                    children: <Widget>[
                      Positioned(left: 0, bottom: 0, child: triangle),
                      messagebody,
                    ],
                  );
                }

                if (reverse) {
                  return Row(
                    mainAxisAlignment: MainAxisAlignment.end,
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: <Widget>[
                      Padding(
                        padding: const EdgeInsets.all(8.0),
                        child: message,
                      ),
                      avatar,
                    ],
                  );
                } else {
                  return Row(
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: <Widget>[
                      avatar,
                      Padding(
                        padding: const EdgeInsets.all(8.0),
                        child: message,
                      ),
                    ],
                  );
                }
              },
            ),
          ),
          Divider(height: 2.0),
          textInput
        ],
      ),
    );
  }

  static const _quickBloxChannel = const MethodChannel('quickbloxbridge');

  Future<void> _getBatteryInformation(var text) async {
    String batteryPercentage;
    try {
      var result = await _quickBloxChannel
          .invokeMethod('getChatMessageEntered', {"message": text});

      batteryPercentage = 'Battery level at $result%';
    } on PlatformException catch (e) {
      batteryPercentage = "Failed to get battery level: '${e.message}'.";
    }

    setState(() {
      _batteryPercentage = batteryPercentage;
    });
  }

  Future<void> _getChatHistory() async {
    getChatHistoryFromNative().then((val) => setState(() {
          _messages = val.cast<String>().toList();
        }));

    // messages =
    //     _quickBloxChannel.setMethodCallHandler(myUtilsHandler(methodCall));
    // messages = getChatHistory();
    // try {
    //   var result = await _quickBloxChannel.invokeMethod('getChatHistory');

    //   if (result is List<String>) {
    //     messages = result;
    //     log('data: $messages[0]');
    //   }
    // } on PlatformException catch (e) {
    //   // batteryPercentage = "Failed to get battery level: '${e.message}'.";
    // }

    print('Inside chat history');

    // getChatHistoryFromNative();
  }

  Future<List> getChatHistoryFromNative() {
    var completer = new Completer<List>();
    _quickBloxChannel.setMethodCallHandler((MethodCall call) {
      switch (call.method) {
        case 'getChatHistory':
          {
            try {
              List messages = call.arguments;
              completer.complete(messages);
              // List messages = call.arguments;

              // setState(() {
              //   _messages = messages.cast<String>();
              // });
            } catch (e) {
              print(e.toString());
            }
          }
      }
    });
    return completer.future;
  }
}

class Triangle extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    var paint = Paint()..color = Colors.amber;

    var path = Path();
    path.lineTo(10, 0);
    path.lineTo(0, -10);
    path.lineTo(-10, 0);
    canvas.drawPath(path, paint);
  }

  @override
  bool shouldRepaint(CustomPainter oldDelegate) {
    return true;
  }
}
