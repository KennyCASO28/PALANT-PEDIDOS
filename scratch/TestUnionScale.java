import javafx.scene.shape.*;
import javafx.scene.Group;
public class TestUnionScale {
    public static void main(String[] args) {
        Rectangle r = new Rectangle(0, 0, 100, 100);
        Group g = new Group(r);
        g.setTranslateX(500);
        Shape s = Shape.union(r, new Path());
        System.out.println("Union bounds: " + s.getBoundsInLocal());
    }
}
