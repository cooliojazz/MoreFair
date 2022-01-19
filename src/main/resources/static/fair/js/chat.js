let messageTemplate = {
    username: "Chad, the Listener",
    message: "Sorry, I'm currently resting my ears. If you want to be heard, head over into our Discord. https://discord.gg/Ud7UfFJmYj ",
    timesAsshole: 0,
    accountId: 0
}

let chatData = {
    messages: [messageTemplate],
    currentChatNumber: 1
}

function initChat(ladderNum) {
    stompClient.send("/app/chat/init/" + ladderNum, {}, JSON.stringify({
        'uuid': getCookie("_uuid")
    }));
}

function handleChatInit(message) {
    if (message.status === "OK") {
        if (message.content) {
            chatData = message.content;
        }
    }
    updateChat();
}

function sendMessage() {
    let messageInput = $('#messageInput')[0];
    const message = messageInput.value;
    if (message === "") return;
    messageInput.value = "";

    stompClient.send("/app/chat/post/" + chatData.currentChatNumber, {}, JSON.stringify({
        'uuid': getCookie("_uuid"),
        'content': message
    }));
}

function handleChatUpdates(message) {
    if (message) {
        chatData.messages.unshift(message);
        if (chatData.messages.length > 30) chatData.messages.pop();
    }
    updateChat();
}

function changeChatRoom(ladderNum) {
    chatSubscription.unsubscribe();
    chatSubscription = stompClient.subscribe('/topic/chat/' + ladderNum,
        (message) => handleChatUpdates(JSON.parse(message.body)), {uuid: getCookie("_uuid")});
    initChat(ladderNum);
}

function updateChat() {
    let body = $('#messagesBody')[0];
    body.innerHTML = "";
    for (let i = 0; i < chatData.messages.length; i++) {
        let message = chatData.messages[i];
        let row = body.insertRow();
        let assholeTag = (message.timesAsshole < infoData.assholeTags.length) ?
            infoData.assholeTags[message.timesAsshole] : infoData.assholeTags[infoData.assholeTags.length - 1];
        row.insertCell(0).innerHTML = message.username + ": " + assholeTag;
        row.cells[0].classList.add('overflow-hidden')
        row.cells[0].style.whiteSpace = 'nowrap';
        row.insertCell(1).innerHTML = "&nbsp;" + message.message;
    }
}

function updateChatUsername(event) {
    chatData.messages.forEach(message => {
        if (event.accountId === message.accountId) {
            message.username = event.data;
        }
    })
    updateChat();
}