package ui.views;

import model.SavedCard;
import model.User;
import storage.CardStorage;
import storage.MessageStorage;
import storage.UserStorage;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class UserCollectionsView extends JPanel {
    private final User currentUser;

    private final DefaultListModel<User> userListModel = new DefaultListModel<>();
    private final JList<User> userList = new JList<>(userListModel);

    private final OtherUserCollectionView collectionView = new OtherUserCollectionView();
    private User selectedUser;

    public UserCollectionsView(User currentUser) {
        this.currentUser = currentUser;

        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(40, 40, 50));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(30, 30, 40));
        header.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));

        JLabel title = new JLabel("Users' Collections");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 18));
        header.add(title, BorderLayout.WEST);

        add(header, BorderLayout.NORTH);

        setupUserList();

        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setPreferredSize(new Dimension(220, 0));
        userScroll.setBorder(BorderFactory.createLineBorder(new Color(70, 70, 90)));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, userScroll, collectionView);
        splitPane.setResizeWeight(0.25);

        add(splitPane, BorderLayout.CENTER);

        refreshUsers();
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

        if (!userListModel.isEmpty()) {
            if (selectedUser != null) {
                for (int i = 0; i < userListModel.size(); i++) {
                    if (userListModel.getElementAt(i).id.equals(selectedUser.id)) {
                        userList.setSelectedIndex(i);
                        return;
                    }
                }
            }
            userList.setSelectedIndex(0);
        } else {
            selectedUser = null;
            collectionView.refresh();
        }
    }

    private void setupUserList() {
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setBackground(new Color(50, 50, 65));
        userList.setForeground(Color.WHITE);

        userList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);

                if (value instanceof User user) {
                    label.setText(user.username);
                }

                label.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
                return label;
            }
        });

        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedUser = userList.getSelectedValue();
                collectionView.refresh();
            }
        });
    }

    private class OtherUserCollectionView extends SavedCardsView {
        @Override
        protected String getTitle() {
            if (selectedUser == null) {
                return "Select a user";
            }
            return selectedUser.username + "'s Collection";
        }
        @Override
        protected boolean showCardDetailsOnClick() {
            return false;
        }

        @Override
        protected String getEmptyMessage() {
            if (selectedUser == null) {
                return "Select a user on the left to view their collection.";
            }
            return selectedUser.username + " has no cards in their collection yet.";
        }

        @Override
        protected List<SavedCard> loadCards() {
            if (selectedUser == null) {
                return Collections.emptyList();
            }
            return CardStorage.loadCollectionForUser(selectedUser.id);
        }

        @Override
        protected void removeCard(String id) {
            JOptionPane.showMessageDialog(
                    this,
                    "You cannot remove cards from another user's collection."
            );
        }

        @Override
        protected void onCardClicked(SavedCard card) {
            if (selectedUser == null || card == null) {
                return;
            }
            showCardActions(selectedUser, card);
        }

        @Override
        protected boolean canRemoveCards() {
            return false;
        }
    }

    private void showCardActions(User owner, SavedCard wantedCard) {
        String[] options = {"Trade for this card", "Buy this card", "Cancel"};

        int choice = JOptionPane.showOptionDialog(
                this,
                "What would you like to do with " + wantedCard.name + "?",
                "Card Options",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]
        );

        if (choice == 0) {
            sendTradeRequest(owner, wantedCard);
        } else if (choice == 1) {
            sendBuyRequest(owner, wantedCard);
        }
    }

    private void sendTradeRequest(User owner, SavedCard wantedCard) {
        List<SavedCard> myCards = CardStorage.loadCollection();

        if (myCards.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "You need at least one card in your collection before sending a trade request."
            );
            return;
        }

        SavedCard offeredCard = chooseMyCard(myCards);
        if (offeredCard == null) {
            return;
        }

        JTextArea noteArea = new JTextArea(5, 20);
        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(noteArea);

        String[] options = {"Send", "Skip", "Cancel"};

        int choice = JOptionPane.showOptionDialog(
                this,
                scrollPane,
                "Add a note (optional)",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]
        );

        if (choice == 2 || choice == JOptionPane.CLOSED_OPTION) {
            return;
        }

        String note;
        if (choice == 0) {
            note = noteArea.getText().trim();
        } else {
            note = "";
        }

        StringBuilder body = new StringBuilder();
        body.append("TRADE REQUEST\n\n");
        body.append("Hi ").append(owner.username).append(",\n");
        body.append("I'm interested in your card: ").append(wantedCard.name).append("\n");
        body.append("I would like to offer my card: ").append(offeredCard.name).append("\n");

        if (wantedCard.marketPrice != null && !wantedCard.marketPrice.isBlank()) {
            body.append("Your card market price: ").append(wantedCard.marketPrice).append("\n");
        }

        if (offeredCard.marketPrice != null && !offeredCard.marketPrice.isBlank()) {
            body.append("My offered card market price: ").append(offeredCard.marketPrice).append("\n");
        }

        if (!note.isEmpty()) {
            body.append("\nNote: ").append(note).append("\n");
        }

        body.append("\nLet me know if you'd like to trade.");

        MessageStorage.sendMessage(currentUser.id, owner.id, body.toString());

        JOptionPane.showMessageDialog(
                this,
                "Trade request sent to " + owner.username + "."
        );
    }

    private void sendBuyRequest(User owner, SavedCard wantedCard) {
        String amount = JOptionPane.showInputDialog(
                this,
                "Enter your offer amount for " + wantedCard.name + ":",
                "Buy Request",
                JOptionPane.PLAIN_MESSAGE
        );

        if (amount == null || amount.trim().isEmpty()) {
            return;
        }

        JTextArea noteArea = new JTextArea(5, 20);
        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(noteArea);

        String[] options = {"Send", "Skip", "Cancel"};

        int choice = JOptionPane.showOptionDialog(
                this,
                scrollPane,
                "Add a note (optional)",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]
        );

        if (choice == 2 || choice == JOptionPane.CLOSED_OPTION) {
            return;
        }

        String note;
        if (choice == 0) {
            note = noteArea.getText().trim();
        } else {
            note = "";
        }

        StringBuilder body = new StringBuilder();
        body.append("PURCHASE OFFER\n\n");
        body.append("Hi ").append(owner.username).append(",\n");
        body.append("I'm interested in buying your card: ").append(wantedCard.name).append("\n");
        body.append("My offer: ").append(amount.trim()).append("\n");

        if (wantedCard.marketPrice != null && !wantedCard.marketPrice.isBlank()) {
            body.append("Listed market price: ").append(wantedCard.marketPrice).append("\n");
        }

        if (!note.isEmpty()) {
            body.append("\nNote: ").append(note).append("\n");
        }

        body.append("\nLet me know if you're interested in selling it.");

        MessageStorage.sendMessage(currentUser.id, owner.id, body.toString());

        JOptionPane.showMessageDialog(
                this,
                "Purchase offer sent to " + owner.username + "."
        );
    }

    private SavedCard chooseMyCard(List<SavedCard> myCards) {
        DefaultListModel<SavedCard> model = new DefaultListModel<>();
        for (SavedCard card : myCards) {
            model.addElement(card);
        }

        JList<SavedCard> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);

                if (value instanceof SavedCard card) {
                    label.setText(card.name + "  (" + safe(card.marketPrice) + ")");
                }
                return label;
            }
        });

        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setPreferredSize(new Dimension(350, 220));

        int result = JOptionPane.showConfirmDialog(
                this,
                scrollPane,
                "Choose one of your cards to offer",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            return list.getSelectedValue();
        }

        return null;
    }

    private String safe(String value) {
        return (value == null || value.isBlank()) ? "N/A" : value;
    }
}