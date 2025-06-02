public class TreeNode {
    public String attribute;
    public double threshold;
    public TreeNode left;
    public TreeNode right;
    public String label;

    public boolean isLeaf() {
        return label != null;
    }
}
