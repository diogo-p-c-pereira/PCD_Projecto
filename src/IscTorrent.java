public class IscTorrent {
    public static void main(String[] args) {    //args[0] -> Port Number | args[1] -> Folder Name
        if (args.length!=2)
            throw new IllegalArgumentException ( "Exactly two arguments needed: Port and Folder name." );
        Node node = new Node(Integer.parseInt(args[0]), args[1]);
        GUI gui = new GUI(node);
        gui.open();
    }
}
