package client;

import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class User extends JPanel {
	public String name;
	private JLabel nameField;

	public User(String name) {
		this.name = name;
		nameField = new JLabel(name);
		//nameField.setEditable(false);
		this.setLayout(new BorderLayout());
		this.add(nameField);
	}
	
	public String getName() {
		return name;
	}

}