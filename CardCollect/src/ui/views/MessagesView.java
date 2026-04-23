package ui.views;

import model.Message;
import model.User;
import storage.MessageStorage;
import storage.UserStorage;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class MessagesView extends JPanel {
    private final User currentUser;
    private final DefaultListModel<User> userListModel = new DefaultListModel<>();
    private final JList<User> userList = new JList<>(userListModel);
    private final JPanel conversationPanel = new JPanel();
    private final JTextArea messageInput = new JTextArea(3, 20);
    private final JLabel conversationTitle = new JLabel("Select an account to start messaging");
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("MMM d, yyyy h:mm a");

    // refresh for new messages
    private Timer refreshTimer;
    private String lastSeenMessageId = null;
    private final JScrollPane conversationScroll;

    public MessagesView(User currentUser) {
        this.currentUser = currentUser;
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(40, 40, 50));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(30, 30, 40));
        header.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));

        JLabel title = new JLabel("💬 Messages");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 18));
        header.add(title, BorderLayout.WEST);

        add(header, BorderLayout.NORTH);

        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setBackground(new Color(50, 50, 65));
        userList.setForeground(Color.WHITE);
        userList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof User user) {
                    label.setText(user.username + (user.id.equals(currentUser.id) ? " (You)" : ""));
                }
                label.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
                return label;
            }
        });
        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                refreshConversation();
            }
        });

        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setPreferredSize(new Dimension(220, 0));

        JPanel conversationContainer = new JPanel(new BorderLayout(8, 8));
        conversationContainer.setBackground(new Color(40, 40, 50));

        conversationTitle.setForeground(Color.WHITE);
        conversationTitle.setFont(new Font("Arial", Font.BOLD, 16));
        conversationTitle.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        conversationContainer.add(conversationTitle, BorderLayout.NORTH);

        conversationPanel.setLayout(new BoxLayout(conversationPanel, BoxLayout.Y_AXIS));
        conversationPanel.setBackground(new Color(40, 40, 50));
        conversationScroll = new JScrollPane(conversationPanel);        conversationScroll.setBorder(BorderFactory.createLineBorder(new Color(70, 70, 90)));
        conversationContainer.add(conversationScroll, BorderLayout.CENTER);

        JPanel composer = new JPanel(new BorderLayout(8, 8));
        composer.setBackground(new Color(40, 40, 50));
        messageInput.setLineWrap(true);
        messageInput.setWrapStyleWord(true);
        JScrollPane inputScroll = new JScrollPane(messageInput);
        JButton sendButton = new JButton("Send");
        sendButton.setBackground(new Color(60, 80, 120));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setBorderPainted(false);
        sendButton.addActionListener(e -> sendCurrentMessage());
        composer.add(inputScroll, BorderLayout.CENTER);
        composer.add(sendButton, BorderLayout.EAST);
        conversationContainer.add(composer, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, userScroll, conversationContainer);
        splitPane.setResizeWeight(0.25);
        add(splitPane, BorderLayout.CENTER);

        refreshUsers();
        startAutoRefresh();
    }
    private void startAutoRefresh() {
        refreshTimer = new Timer(1000, e -> {
            User selectedUser = userList.getSelectedValue();
            if (selectedUser == null) return;

            List<Message> messages = MessageStorage.getConversation(currentUser.id, selectedUser.id);
            String newestId = messages.isEmpty() ? null : messages.get(messages.size() - 1).id;

            if (!java.util.Objects.equals(newestId, lastSeenMessageId)) {
                lastSeenMessageId = newestId;
                refreshConversation();
            }
        });

        refreshTimer.start();
    }

    public void refreshUsers() {
        userListModel.clear();
        List<User> users = UserStorage.getAllUsers();
        users.sort(Comparator.comparing(u -> u.username.toLowerCase()));
        for (User user : users) {
            if (!user.id.equals(currentUser.id)) {
                userListModel.addElement(user);
            }
        }

        if (!userListModel.isEmpty() && userList.getSelectedIndex() == -1) {
            userList.setSelectedIndex(0);
        } else {
            refreshConversation();
        }
    }

    private void sendCurrentMessage() {
        User selectedUser = userList.getSelectedValue();
        if (selectedUser == null) {
            JOptionPane.showMessageDialog(this, "Select another account first.");
            return;
        }

        String text = messageInput.getText().trim();
        if (text.isEmpty()) {
            return;
        }

        MessageStorage.sendMessage(currentUser.id, selectedUser.id, text);
        messageInput.setText("");

        List<Message> messages = MessageStorage.getConversation(currentUser.id, selectedUser.id);
        lastSeenMessageId = messages.isEmpty() ? null : messages.get(messages.size() - 1).id;

        refreshConversation();
    }

    private void refreshConversation() {
        conversationPanel.removeAll();

        User selectedUser = userList.getSelectedValue();
        if (selectedUser == null) {
            conversationTitle.setText("No other accounts found");
            lastSeenMessageId = null;

            JLabel empty = new JLabel("Create a second account to start messaging.");
            empty.setForeground(Color.LIGHT_GRAY);
            empty.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            conversationPanel.add(empty);
        } else {
            conversationTitle.setText("Conversation with " + selectedUser.username);
            List<Message> messages = MessageStorage.getConversation(currentUser.id, selectedUser.id);

            lastSeenMessageId = messages.isEmpty() ? null : messages.get(messages.size() - 1).id;

            if (messages.isEmpty()) {
                JLabel empty = new JLabel("No messages yet. Send the first one.");
                empty.setForeground(Color.LIGHT_GRAY);
                empty.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                conversationPanel.add(empty);
            } else {
                for (Message message : messages) {
                    conversationPanel.add(buildBubble(message));
                    conversationPanel.add(Box.createRigidArea(new Dimension(0, 8)));
                }
            }
        }

        conversationPanel.revalidate();
        conversationPanel.repaint();

        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = conversationScroll.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    private JPanel buildBubble(Message message) {
        boolean mine = message.fromUserId.equals(currentUser.id);
        JPanel row = new JPanel(new FlowLayout(mine ? FlowLayout.RIGHT : FlowLayout.LEFT));
        row.setOpaque(false);

        JPanel bubble = new JPanel();
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBackground(mine ? new Color(70, 110, 160) : new Color(60, 60, 75));
        bubble.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        bubble.setMaximumSize(new Dimension(420, Integer.MAX_VALUE));

        JLabel sender = new JLabel(mine ? "You" : safeUsername(message.fromUserId));
        sender.setForeground(new Color(230, 230, 240));
        sender.setFont(new Font("Arial", Font.BOLD, 12));

        JTextArea body = new JTextArea(message.body);
        body.setEditable(false);
        body.setOpaque(false);
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        body.setForeground(Color.WHITE);
        body.setFont(new Font("Arial", Font.PLAIN, 13));

        JLabel stamp = new JLabel(timeFormat.format(new Date(message.timestamp)));
        stamp.setForeground(new Color(215, 215, 225));
        stamp.setFont(new Font("Arial", Font.PLAIN, 11));

        bubble.add(sender);
        bubble.add(Box.createRigidArea(new Dimension(0, 4)));
        bubble.add(body);
        bubble.add(Box.createRigidArea(new Dimension(0, 4)));
        bubble.add(stamp);

        row.add(bubble);
        return row;
    }

    private String safeUsername(String userId) {
        User user = UserStorage.findById(userId);
        return user != null ? user.username : "Unknown user";
    }
}
