package jd.captcha.easy;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import jd.utils.locale.JDL;

import jd.gui.swing.jdgui.settings.JDLabelListRenderer;

import jd.gui.swing.GuiRunnable;

import jd.utils.JDUtilities;

import jd.nutils.Screen;

import jd.gui.userio.DummyFrame;

public class EasyCaptchaTool {

    private static EasyFile getCaptchaMethode() {
        return new GuiRunnable<EasyFile>() {
            public EasyFile runSave() {
                final EasyFile ef = new EasyFile();
                final JDialog dialog = new JDialog(DummyFrame.getDialogParent());
                dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                dialog.setTitle(JDL.L("easycaptcha.tool.title", "EasyCaptcha"));
                dialog.setModal(true);

                JPanel box = new JPanel(new GridLayout(2, 1));
                JButton btl = new JButton(JDL.L("easycaptcha.tool.loadmethode", "Load Methode"));
                btl.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        new GuiRunnable<Object>() {
                            public Object runSave() {
                                final JDialog cHosterDialog = new JDialog(DummyFrame.getDialogParent());
                                cHosterDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                                cHosterDialog.setTitle(JDL.L("easycaptcha.tool.mothodedialog.title", "EasyCaptcha Methodes"));
                                cHosterDialog.setModal(true);
                                Box box = new Box(BoxLayout.Y_AXIS);

                                JPanel pa = new JPanel(new GridLayout(2, 1));
                                
                                pa.add(new JLabel(JDL.L("easycaptcha.tool.mothodedialog.selectmethode", "select the methode:")));
                                EasyFile[] paths = new EasyFile(JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/" + JDUtilities.getJACMethodsDirectory()).listFiles();
                                
                                final JComboBox combox = new JComboBox(paths);
                                combox.setRenderer(new JDLabelListRenderer());
                                combox.setMinimumSize(new Dimension(24,70));
                                pa.add(combox);
                                box.add(pa);
                                pa = new JPanel(new GridLayout(1, 2));
                                JButton ok = new JButton(JDL.L("gui.btn_ok", "OK"));
                                pa.add(ok);
                                ok.addActionListener(new ActionListener() {

                                    public void actionPerformed(ActionEvent e) {
                                        EasyFile ef2 = (EasyFile) combox.getSelectedItem();
                                        if (ef2 != null) {
                                            ef.file = ef2.file;
                                            dialog.dispose();
                                        }                                    }
                                });
                                
                                JButton cancel = new JButton(JDL.L("gui.btn_cancel", "Cancel"));
                                pa.add(cancel);
                                cancel.addActionListener(new ActionListener() {

                                    public void actionPerformed(ActionEvent e) {
                                        cHosterDialog.dispose();
                                    }
                                });
                                box.add(pa);

                                cHosterDialog.add(box);
                                cHosterDialog.pack();
                                cHosterDialog.setLocation(Screen.getCenterOfComponent(DummyFrame.getDialogParent(), cHosterDialog));
                                cHosterDialog.setVisible(true);

                                return null;
                            }
                        }.waitForEDT();

                    }
                });

                box.add(btl);
                JButton btc = new JButton(JDL.L("easycaptcha.tool.createmethode", "create methode"));
                btc.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        new GuiRunnable<Object>() {
                            public Object runSave() {
                                final JDialog cHosterDialog = new JDialog(DummyFrame.getDialogParent());
                                cHosterDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                                cHosterDialog.setTitle(JDL.L("easycaptcha.tool.title", "EasyCaptcha"));
                                cHosterDialog.setModal(true);
                                JPanel box = new JPanel(new GridLayout(4, 2));
                                final JTextField tfHoster = new JTextField();
                                box.add(new JLabel(JDL.L("gui.column_host", "Host")+":"));
                                box.add(tfHoster);
                                final JTextField tfName = new JTextField();
                                box.add(new JLabel(JDL.L("gui.config.jac.column.author", "Author")+":"));
                                box.add(tfName);
                                final JSpinner spMaxLetters = new JSpinner(new SpinnerNumberModel(4, 1, 40, 1));
                                box.add(new JLabel(JDL.L("easycaptcha.tool.maxletternum", "Maximal letter number")+":"));
                                box.add(spMaxLetters);
                                JButton ok = new JButton(JDL.L("gui.btn_ok", "OK"));
                                box.add(ok);
                                ok.addActionListener(new ActionListener() {

                                    public void actionPerformed(ActionEvent e) {
                                        if (tfHoster.getText() != null && !tfHoster.getText().matches("\\s*")) {

                                            ef.file = new File(JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/" + JDUtilities.getJACMethodsDirectory(), tfHoster.getText());
                                            dialog.dispose();
                                            cHosterDialog.dispose();
                                            if(!CreateHoster.create(ef, tfName.getText(), (Integer) spMaxLetters.getValue()))
                                                    ef.file=null;

                                        } else {
                                            JOptionPane.showConfirmDialog(null, JDL.L("easycaptcha.tool.warning.hostnamemissing", "the hostname is missing"), JDL.L("easycaptcha.tool.warning.hostnamemissing", "the hostname is missing"), JOptionPane.CLOSED_OPTION, JOptionPane.WARNING_MESSAGE);
                                        }
                                    }
                                });
                                JButton cancel = new JButton(JDL.L("gui.btn_cancel", "Cancel"));
                                box.add(cancel);
                                cancel.addActionListener(new ActionListener() {

                                    public void actionPerformed(ActionEvent e) {
                                        cHosterDialog.dispose();
                                    }
                                });
                                cHosterDialog.add(box);
                                cHosterDialog.pack();
                                cHosterDialog.setLocation(Screen.getCenterOfComponent(DummyFrame.getDialogParent(), cHosterDialog));
                                cHosterDialog.setVisible(true);
                                return null;
                            }
                        }.waitForEDT();
                    }
                });

                box.add(btc);
                dialog.add(box);
                dialog.pack();
                dialog.setLocation(Screen.getCenterOfComponent(DummyFrame.getDialogParent(), dialog));
                dialog.setVisible(true);
                if (ef.file != null)
                    return ef;
                else
                    return null;
            }
        }.getReturnValue();

    }

    public static void main(String[] args) {

        EasyCaptchaTool.getCaptchaMethode();
        System.exit(0);

    }

}
