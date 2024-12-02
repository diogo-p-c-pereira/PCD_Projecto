import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import javax.swing.*;


public class GUI  {
    private Node node; //Current Node
    private JFrame frame;

    //// Panels
    private JPanel northPanel;
    private JPanel eastPanel;

    //// Top Search Bar
    private JTextField searchText;
    private JButton searchButton;

    //// FileSearchResult List
    private JList<FileSearch> searchList;

    //// East Buttons
    private JButton downloadButton;
    private JButton connectButton;

    public GUI(Node node) {
        this.node = node;
        frame = new JFrame(node.toString());
        addFrameContent();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
    }

    private void addFrameContent () {
        frame.setLayout (new BorderLayout());

        //Top Search Bar
        northPanel = new JPanel();
        frame.add(northPanel, BorderLayout.NORTH);
        northPanel.setLayout(new GridLayout(1,3));

        JLabel label = new JLabel("Texto a procurar:            ");
        northPanel.add(label);

        searchText = new JTextField("");
        searchText.setColumns(20);
        northPanel.add(searchText);

        searchButton = new JButton ("Procurar");
        searchButton.addActionListener(_ -> node.search(searchText.getText()));
        northPanel.add(searchButton);

        //File Search Result List
        searchList = new JList<>(node.getCurrentSearchResults());
        JScrollPane scrollPane = new JScrollPane(searchList);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.black));
        frame.add(scrollPane, BorderLayout.CENTER);

        //East Buttons Panel
        eastPanel = new JPanel();
        eastPanel.setLayout(new GridLayout(2,1));
        frame.add(eastPanel, BorderLayout.EAST);

        //Download Button
        downloadButton = new JButton ("Descarregar");
        downloadButton.addActionListener(_ -> {
            /*TODO ir vendo*/
            FileSearch res = searchList.getSelectedValue();
            node.startDownload(res.getList());
        });
        eastPanel.add(downloadButton);

        //Connect Button
        connectButton = new JButton("Ligar a nó");
        connectButton.addActionListener(_ -> newConnectionPrompt());
        eastPanel.add(connectButton);
    }

    //// New Connection Pop-up
    private void newConnectionPrompt() {
        JDialog jdialog = new JDialog(frame, true);
        jdialog.setLayout(new FlowLayout());
        jdialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        //Address and Port Labels and TextFields
        jdialog.add(new JLabel("Endereço:"));
        JTextField address = new JTextField("", 10);
        jdialog.add(address);
        jdialog.add(new JLabel("Porta:"));
        JTextField port = new JTextField("", 3);
        jdialog.add(port);

        //Cancel Button
        JButton cancel = new JButton("Cancelar");
        cancel.addActionListener(_ -> jdialog.dispose());
        jdialog.add(cancel);

        //OK Button
        JButton save = new JButton("OK");
        save.addActionListener(_ -> {
            if(node.newConnection(address.getText(), Integer.parseInt(port.getText()))) {
                JOptionPane.showMessageDialog(jdialog, "Conectado com sucesso");
            }else{
                JOptionPane.showMessageDialog(jdialog, "Falha na conecção");
            }
            jdialog.dispose();
        });
        jdialog.add(save);

        jdialog.setLocationRelativeTo(searchList);
        jdialog.pack();
        jdialog.setVisible(true);
    }

    public void open () {
        frame.setVisible(true);
    }

    //TODO GUI for Download Complete Pop-up
}