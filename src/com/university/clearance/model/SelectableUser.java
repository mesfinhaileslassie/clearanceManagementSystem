package com.university.clearance.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class SelectableUser extends User {
    private final BooleanProperty selected;
    
    public SelectableUser(int id, String username, String fullName, String role, 
                         String email, String department) {
        super(id, username, fullName, role, email, department);
        this.selected = new SimpleBooleanProperty(false);
    }
    
    public SelectableUser(User user) {
        super(user.getId(), user.getUsername(), user.getFullName(), 
              user.getRole(), user.getEmail(), user.getDepartment());
        this.selected = new SimpleBooleanProperty(false);
        // Copy all properties from the user
        this.setStatus(user.getStatus());
        this.setYearLevel(user.getYearLevel());
        this.setPhone(user.getPhone());
        this.setClearanceStatus(user.getClearanceStatus());
        this.setCanReapply(user.isCanReapply());
        this.setPassword(user.getPassword()); // If needed
    }
    
    public boolean isSelected() {
        return selected.get();
    }
    
    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }
    
    public BooleanProperty selectedProperty() {
        return selected;
    }
    
    // Override toString for debugging
    @Override
    public String toString() {
        return "SelectableUser{" +
                "username='" + getUsername() + '\'' +
                ", fullName='" + getFullName() + '\'' +
                ", selected=" + isSelected() +
                '}';
    }
}