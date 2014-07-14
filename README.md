koku-android-tools
==================

Collection of useful tools

at.kokutoru.ui
----
```Java
import at.kokutoru.tool.ui.UI;
import at.kokutoru.tool.ui.UI_ELEMENT;
import at.kokutoru.tool.ui.UI_LAYOUT;
import at.kokutoru.tool.ui.UI_SAVESTATE;

@UI_LAYOUT(id = R.layout.activity_scene_transitions)
public class MyActivity extends Activity {
    @UI_SAVESTATE
    String data = "";


    @UI_ELEMENT(name = "my_button_a", onclick = "my_button_click")
    Button myButtonA;

    /* alternative:
    @UI_ELEMENT(onclick = "my_button_click")
    Button my_button_a;
     */

    protected void my_button_click(View view) {
        Log.e("A", "CLICK");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        UI.save(this, outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        UI.restore(this, savedInstanceState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(UI.init(this, savedInstanceState));
        data = "TEST";
    }
}
```
