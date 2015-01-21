package com.intel.galileo.flash.tool;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.Font;
import javax.swing.JButton;
import java.awt.event.ActionEvent;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;

public class About {

	private JFrame frame;

	/**
	 * Launch the application.
	 */
	public static void showMe(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				
				try {
									        
					About window = new About();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public About() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setResizable(false);
		frame.setBounds(100, 100, 527, 295);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		JButton btnExitButton = new JButton("Exit");
		
		btnExitButton.addActionListener(new java.awt.event.ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				// TODO Auto-generated method stub
				frame.dispose(); // leaves the About Dialog
			}
		
		});

		String HTMLlabelStr = "<html><div style='text-align:center'> "
				              + "<h1><strong>Galileo Firmware Update "
		                      + FirmwareUpdateTool.capVersion+"</h1> <p><br>Intel Galileo Firmware Update Tool  "
				              + FirmwareUpdateTool.appVersion+" all rights reserved</p> </div> </html>";
		
		JLabel lblNewLabel = new JLabel(HTMLlabelStr);
		lblNewLabel.setFont(new Font("Tahoma", Font.BOLD, 17));
		lblNewLabel.setHorizontalAlignment(SwingConstants.CENTER);
		GroupLayout groupLayout = new GroupLayout(frame.getContentPane());
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap(36, Short.MAX_VALUE)
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addGroup(Alignment.TRAILING, groupLayout.createSequentialGroup()
							.addComponent(btnExitButton, GroupLayout.PREFERRED_SIZE, 108, GroupLayout.PREFERRED_SIZE)
							.addGap(205))
						.addGroup(Alignment.TRAILING, groupLayout.createSequentialGroup()
							.addComponent(lblNewLabel, GroupLayout.PREFERRED_SIZE, 460, GroupLayout.PREFERRED_SIZE)
							.addGap(25))))
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.TRAILING)
				.addGroup(groupLayout.createSequentialGroup()
					.addGap(23)
					.addComponent(lblNewLabel, GroupLayout.DEFAULT_SIZE, 145, Short.MAX_VALUE)
					.addGap(30)
					.addComponent(btnExitButton)
					.addGap(46))
		);
		frame.getContentPane().setLayout(groupLayout);
	}
}
