class ChatListingData {
  String chatMessage;
  bool isMessageOfCurrentUser;

  ChatListingData(this.chatMessage, this.isMessageOfCurrentUser);

  dynamic get getChatMessage {
    return chatMessage;
  }

  bool get checkMessageOfCurrentUser {
    return isMessageOfCurrentUser;
  }
}
