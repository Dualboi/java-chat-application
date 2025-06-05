let username = "";

function sendMessage(message) {
    fetch('/api/webchat/messages', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ user: username, message: message })
    });
}

function fetchMessages() {
    fetch('/api/webchat/messages')
        .then(response => response.json())
        .then(data => {
            const chatMessages = document.getElementById("chatMessages");
            chatMessages.innerHTML = "";
            data.forEach(msg => {
                chatMessages.innerHTML += `<div>${msg}</div>`;
            });
            chatMessages.scrollTop = chatMessages.scrollHeight;
        });
}

document.addEventListener("DOMContentLoaded", function() {
    document.getElementById("chatForm").addEventListener("submit", function(e) {
        e.preventDefault();
        const message = document.getElementById("message").value;
        if (message) {
            sendMessage(message);
            document.getElementById("message").value = "";
        }
    });
});

// Updated login function with password validation
window.login = function() {
    username = document.getElementById("username").value;
    const password = document.getElementById("password").value;

    fetch('/api/webchat/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: username, password: password })
    })
    .then(response => response.json())
    .then(data => {
        if (data.valid) {
            document.getElementById("loginOverlay").style.display = "none";
            document.getElementById("chatSection").style.display = "block";
            setInterval(fetchMessages, 1000); // Poll every second
            fetchMessages();
        } else {
            alert("Invalid username or password. Please try again.");
        }
    })
    .catch(() => {
        alert("Error connecting to server.");
    });
};

window.addEventListener("beforeunload", function () {
    if (username) {
        navigator.sendBeacon(
            "/api/webchat/logout",
            JSON.stringify({ username: username })
        );
    }
});
