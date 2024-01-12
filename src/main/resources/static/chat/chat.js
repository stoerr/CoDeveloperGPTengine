// JavaScript for Co-Developer Chat Interface
$(document).ready(function() {
    // Handler for changing chats
    $('#chatList a').on('click', function (e) {
        e.preventDefault();
        $(this).tab('show');
    });

    // Handler for message input
    $('#messageInput').on('keypress', function (e) {
        if (e.which === 13 && !e.shiftKey) { // Enter key pressed
            e.preventDefault();
            // TODO: Handle sending message
            console.log('Message sent:', $(this).val());
            $(this).val(''); // Clear input
        }
    });
});