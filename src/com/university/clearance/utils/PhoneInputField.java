package com.university.clearance.utils;

import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.application.Platform;

public class PhoneInputField extends HBox {
    private final ComboBox<String> providerComboBox;
    private final TextField phoneField;
    private String currentPrefix = "";
    private boolean isPrefixLocked = false;
    
    public PhoneInputField() {
        super(5);
        
        providerComboBox = new ComboBox<>();
        providerComboBox.getItems().addAll("Ethio Telecom", "Safaricom");
        providerComboBox.setPromptText("Select Provider");
        providerComboBox.setPrefWidth(150);
        
        phoneField = new TextField();
        phoneField.setPromptText("09xxxxxxx");
        phoneField.setPrefWidth(150);
        phoneField.setStyle("-fx-text-fill: #000000;");
        
        getChildren().addAll(providerComboBox, phoneField);
        
        setupEventHandlers();
        
        // Set initial provider and prefix
        providerComboBox.setValue("Ethio Telecom");
        phoneField.setText("09");
        currentPrefix = "09";
        isPrefixLocked = true;
        phoneField.positionCaret(2);
    }
    
    private void setupEventHandlers() {
        // Provider selection handler
        providerComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String newPrefix = "Ethio Telecom".equals(newVal) ? "09" : "07";
                
                // Clear previous digits and set new prefix
                phoneField.setText(newPrefix);
                currentPrefix = newPrefix;
                isPrefixLocked = true;
                
                // Position cursor after prefix
                Platform.runLater(() -> {
                    phoneField.positionCaret(2);
                    phoneField.requestFocus();
                });
                
                // Update tooltip
                updateTooltip();
            }
        });
        
        // Phone field key event handler
        phoneField.setOnKeyPressed(event -> {
            // Get current cursor position
            int caretPos = phoneField.getCaretPosition();
            
            // Prevent backspace/delete in prefix area
            if (caretPos <= 2 && (event.getCode().toString().equals("BACK_SPACE") || 
                                  event.getCode().toString().equals("DELETE"))) {
                showPrefixWarning();
                event.consume();
            }
        });
        
        // Phone field text change handler
        phoneField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                // Get current cursor position before any changes
                int caretPos = phoneField.getCaretPosition();
                
                // Handle prefix protection
                if (newVal.length() < 2) {
                    // User tried to delete prefix - restore it
                    restorePrefix();
                    showPrefixWarning();
                    return;
                }
                
                // Check if prefix was modified
                String prefix = newVal.substring(0, 2);
                if (!prefix.equals(currentPrefix)) {
                    restorePrefix();
                    showPrefixWarning();
                    return;
                }
                
                // Only allow digits
                if (!newVal.matches("\\d*")) {
                    String digitsOnly = newVal.replaceAll("[^\\d]", "");
                    phoneField.setText(digitsOnly);
                    phoneField.positionCaret(Math.min(digitsOnly.length(), caretPos));
                    return;
                }
                
                // Limit total length to 10 characters
                if (newVal.length() > 10) {
                    phoneField.setText(newVal.substring(0, 10));
                    phoneField.positionCaret(10);
                    showLengthWarning();
                    return;
                }
                
                // Check if user entered more than 8 digits after prefix
                if (newVal.length() > 2) {
                    String suffix = newVal.substring(2);
                    if (suffix.length() > 8) {
                        phoneField.setText(currentPrefix + suffix.substring(0, 8));
                        phoneField.positionCaret(10);
                        showLengthWarning();
                        return;
                    }
                }
                
                // Auto-detect provider based on prefix
                if (newVal.startsWith("09")) {
                    providerComboBox.setValue("Ethio Telecom");
                    currentPrefix = "09";
                } else if (newVal.startsWith("07")) {
                    providerComboBox.setValue("Safaricom");
                    currentPrefix = "07";
                }
                
                // Update tooltip based on current state
                updateTooltip();
            }
        });
        
        // Handle mouse clicks to prevent cursor placement in prefix
        phoneField.setOnMouseClicked(event -> {
            int caretPos = phoneField.getCaretPosition();
            if (caretPos < 2) {
                phoneField.positionCaret(2);
            }
        });
        
        // Handle focus to always position cursor after prefix
        phoneField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                Platform.runLater(() -> {
                    if (phoneField.getCaretPosition() < 2) {
                        phoneField.positionCaret(2);
                    }
                });
            }
        });
    }
    
    private void restorePrefix() {
        String currentText = phoneField.getText();
        String suffix = "";
        
        if (currentText.length() > 2) {
            suffix = currentText.substring(2).replaceAll("[^\\d]", "");
            if (suffix.length() > 8) {
                suffix = suffix.substring(0, 8);
            }
        }
        
        phoneField.setText(currentPrefix + suffix);
        phoneField.positionCaret(Math.max(2, currentPrefix.length() + suffix.length()));
    }
    
    private void showPrefixWarning() {
        Tooltip tooltip = new Tooltip("Provider prefix cannot be modified");
        tooltip.setStyle("-fx-font-size: 12px; -fx-text-fill: #e74c3c;");
        phoneField.setTooltip(tooltip);
        
        // Show tooltip near the field
        if (phoneField.getScene() != null && phoneField.getScene().getWindow() != null) {
            tooltip.show(phoneField.getScene().getWindow());
            
            // Auto-hide after 2 seconds
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    Platform.runLater(() -> tooltip.hide());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
    
    private void showLengthWarning() {
        Tooltip tooltip = new Tooltip("Please enter exactly 8 digits after the prefix");
        tooltip.setStyle("-fx-font-size: 12px; -fx-text-fill: #e74c3c;");
        phoneField.setTooltip(tooltip);
        
        if (phoneField.getScene() != null && phoneField.getScene().getWindow() != null) {
            tooltip.show(phoneField.getScene().getWindow());
            
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    Platform.runLater(() -> tooltip.hide());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
    
    private void updateTooltip() {
        String text = phoneField.getText();
        int suffixLength = Math.max(0, text.length() - 2);
        
        if (suffixLength == 0) {
            phoneField.setTooltip(new Tooltip("Enter 8 digits after " + currentPrefix));
        } else if (suffixLength < 8) {
            phoneField.setTooltip(new Tooltip(
                "Enter " + (8 - suffixLength) + " more digits after " + currentPrefix + 
                " (Example: " + currentPrefix + "xxxxxxx)"
            ));
        } else if (suffixLength == 8) {
            phoneField.setTooltip(new Tooltip("✓ Valid phone number: " + text));
        }
    }
    
    public String getPhoneNumber() {
        return phoneField.getText();
    }
    
    public String getProvider() {
        return providerComboBox.getValue();
    }
    
    public void setPhoneNumber(String phone) {
        if (phone != null && phone.length() >= 2) {
            String prefix = phone.substring(0, 2);
            if ("09".equals(prefix)) {
                providerComboBox.setValue("Ethio Telecom");
                currentPrefix = "09";
            } else if ("07".equals(prefix)) {
                providerComboBox.setValue("Safaricom");
                currentPrefix = "07";
            }
            phoneField.setText(phone);
            isPrefixLocked = true;
        }
    }
    
    public ComboBox<String> getProviderComboBox() {
        return providerComboBox;
    }
    
    public TextField getPhoneField() {
        return phoneField;
    }
    
    // Method to manually trigger validation
    public ValidationHelper.ValidationResult validatePhone() {
        String phone = getPhoneNumber();
        String provider = getProvider();
        
        if (phone == null || phone.trim().isEmpty()) {
            return new ValidationHelper.ValidationResult(false, "Required");
        }
        
        if (!phone.matches("^\\d{10}$")) {
            return new ValidationHelper.ValidationResult(false, "Must be 10 digits");
        }
        
        if ("Ethio Telecom".equals(provider) && !phone.startsWith("09")) {
            return new ValidationHelper.ValidationResult(false, "Ethio Telecom requires 09 prefix");
        }
        
        if ("Safaricom".equals(provider) && !phone.startsWith("07")) {
            return new ValidationHelper.ValidationResult(false, "Safaricom requires 07 prefix");
        }
        
        return new ValidationHelper.ValidationResult(true, "Valid ✓");
    }
}