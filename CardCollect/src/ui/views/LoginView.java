package ui.views;

import model.User;
import storage.UserStorage;

import javax.swing.*;
import java.awt.*;

// Login screen shown when the application first launches.
// Contains username and password fields, a login button, and a register button.
// On successful login, it calls the provided callback to switch to the main app views.
//
// Usage in your main frame:
//   LoginView loginView = new LoginView(user -> {
//       // user is the authenticated User object
//       // swap out the login view and load the main app
//       loadMainApp(user);
//   });
//   frame.add(loginView);
public class LoginView extends JPanel {

    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JLabel statusLabel;

    // Callback interface so the main frame knows when login succeeds.
    // The main frame passes a lambda that receives the authenticated User
    // and swaps in the main app views.
    public interface LoginCallback {
        void onLoginSuccess(User user);
    }

    public LoginView(LoginCallback callback) {
        setLayout(new GridBagLayout()); // centers the form panel in the middle of the window
        setBackground(new Color(40, 40, 50));

        // ─── Form container ──────────────────────────────────────────
        // A smaller panel inside the full-window LoginView so the fields
        // aren't stretched across the entire frame
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(new Color(50, 50, 65));
        formPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        formPanel.setMaximumSize(new Dimension(360, 400));

        // ─── Title ───────────────────────────────────────────────────
        JLabel title = new JLabel("CardCollect");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        formPanel.add(title);

        JLabel subtitle = new JLabel("Sign in to your account");
        subtitle.setForeground(new Color(160, 160, 200));
        subtitle.setFont(new Font("Arial", Font.PLAIN, 13));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        formPanel.add(subtitle);
        formPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // ─── Username field ──────────────────────────────────────────
        JLabel usernameLabel = new JLabel("Username");
        usernameLabel.setForeground(new Color(180, 180, 210));
        usernameLabel.setFont(new Font("Arial", Font.BOLD, 12));
        usernameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(usernameLabel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 4)));

        usernameField = new JTextField();
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(usernameField);
        formPanel.add(Box.createRigidArea(new Dimension(0, 12)));

        // ─── Password field ──────────────────────────────────────────
        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setForeground(new Color(180, 180, 210));
        passwordLabel.setFont(new Font("Arial", Font.BOLD, 12));
        passwordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(passwordLabel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 4)));

        passwordField = new JPasswordField();
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(passwordField);
        formPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // ─── Login button ────────────────────────────────────────────
        JButton loginButton = new JButton("Login");
        loginButton.setBackground(new Color(60, 80, 120));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setBorderPainted(false);
        loginButton.setFont(new Font("Arial", Font.BOLD, 13));
        loginButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loginButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        // Hover effect
        loginButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                loginButton.setBackground(new Color(80, 100, 150));
            }

            public void mouseExited(java.awt.event.MouseEvent e) {
                loginButton.setBackground(new Color(60, 80, 120));
            }
        });

        // Attempt login when the button is clicked
        loginButton.addActionListener(e -> attemptLogin(callback));
        formPanel.add(loginButton);
        formPanel.add(Box.createRigidArea(new Dimension(0, 8)));

        // ─── Register button ─────────────────────────────────────────
        JButton registerButton = new JButton("Create Account");
        registerButton.setBackground(new Color(55, 55, 70));
        registerButton.setForeground(Color.WHITE);
        registerButton.setFocusPainted(false);
        registerButton.setBorderPainted(false);
        registerButton.setFont(new Font("Arial", Font.PLAIN, 12));
        registerButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        registerButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        registerButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        // Hover effect
        registerButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                registerButton.setBackground(new Color(75, 75, 95));
            }

            public void mouseExited(java.awt.event.MouseEvent e) {
                registerButton.setBackground(new Color(55, 55, 70));
            }
        });

        // Attempt registration when the button is clicked
        registerButton.addActionListener(e -> attemptRegister(callback));
        formPanel.add(registerButton);
        formPanel.add(Box.createRigidArea(new Dimension(0, 16)));

        // ─── Status label (for errors and success messages) ──────────
        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        formPanel.add(statusLabel);

        // Also allow pressing Enter in the password field to trigger login
        passwordField.addActionListener(e -> attemptLogin(callback));

        add(formPanel);
    }

    // Validates the fields and tries to authenticate against UserStorage.
    // Shows an error message if the fields are empty or credentials are wrong.
    // Calls the callback with the authenticated User on success.
    private void attemptLogin(LoginCallback callback) {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        // Basic field validation
        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password");
            return;
        }

        showSuccess("Signing in...");

        new SwingWorker<User, Void>() {
            private long authStart;

            @Override
            protected User doInBackground() {
                authStart = System.currentTimeMillis();
                return UserStorage.authenticate(username, password);
            }

            @Override
            protected void done() {
                try {
                    long authEnd = System.currentTimeMillis();
                    System.out.println("[TIMING] UserStorage.authenticate(): " + (authEnd - authStart) + " ms");

                    User user = get();
                    if (user != null) {
                        showSuccess("Login successful!");

                        long callbackStart = System.currentTimeMillis();
                        callback.onLoginSuccess(user);
                        long callbackEnd = System.currentTimeMillis();
                        System.out.println("[TIMING] onLoginSuccess callback: " + (callbackEnd - callbackStart) + " ms");
                    } else {
                        showError("Invalid username or password");
                    }
                } catch (Exception e) {
                    showError("Login failed");
                    e.printStackTrace();
                }
            }
        }.execute();
    }

    // Validates the fields and tries to create a new account.
    // Shows an error if the username is taken, or auto-logs in on success.
    private void attemptRegister(LoginCallback callback) {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        // Basic field validation
        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password");
            return;
        }

        if (username.contains("|")) {
            showError("Username cannot contain the '|' character");
            return;
        }

        if (password.length() < 4) {
            showError("Password must be at least 4 characters");
            return;
        }

        long registerStart = System.currentTimeMillis();
        User user = UserStorage.register(username, password);
        long registerEnd = System.currentTimeMillis();
        System.out.println("[TIMING] UserStorage.register(): " + (registerEnd - registerStart) + " ms");

        if (user != null) {
            showSuccess("Account created! Logging in...");

            long callbackStart = System.currentTimeMillis();
            callback.onLoginSuccess(user);
            long callbackEnd = System.currentTimeMillis();
            System.out.println("[TIMING] onLoginSuccess after register: " + (callbackEnd - callbackStart) + " ms");
        } else {
            showError("Username is already taken");
        }
    }

    // Helper to show a red error message in the status label
    private void showError(String message) {
        statusLabel.setForeground(new Color(220, 80, 80));
        statusLabel.setText(message);
    }

    // Helper to show a green success message in the status label
    private void showSuccess(String message) {
        statusLabel.setForeground(new Color(100, 220, 100));
        statusLabel.setText(message);
    }
}
