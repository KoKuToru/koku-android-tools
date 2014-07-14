package at.kokutoru.tool.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by luca on 04.07.14.
 */
public class UI {
    static final long TIME_WARNING_MS = 16; //prints a warning when event takes more than x ms

    private static Context getContext(Activity ac) {
        return ac;
    }

    private static Context getContext(Fragment fc) {
        return fc.getActivity();
    }

    private static <T> Context getContext(T f) {
        if (f instanceof Activity) {
            return getContext((Activity)f);
        } else if (f instanceof Fragment) {
            return getContext((Fragment)f);
        }
        Log.e("UI", "Unknown type");
        return null;
    }

    public static <T> void save(final T instance, final Bundle state) {
        save(instance, state, "");
    }

    public static  <T> void save(final T instance, final Bundle state, String instance_key) {
        Class<?> cls = instance.getClass();
        Field fields[] = cls.getDeclaredFields();
        for(Field field : fields) {
            String name = field.getName();
            if (!field.isAnnotationPresent(UI_SAVESTATE.class)) {
                //only work on UI_SAVESTATEs
                continue;
            }
            //store in bundle
            String key = cls.getCanonicalName()+"."+name;
            if (!instance_key.isEmpty()) {
                key = cls.getCanonicalName()+"["+instance_key+"]."+name;
            }

            boolean acc = field.isAccessible();
            field.setAccessible(true);
            Class<?> typ = field.getType();
            if (Serializable.class.isAssignableFrom(typ)) {
                try {
                    state.putSerializable(key, (Serializable)field.get(instance));
                } catch (IllegalAccessException exp) {
                    //!TODO
                }
            } else {
                //!TODO
                Log.e("UI", cls.getName() + " field " + field.getName() + " does not implement Serializable");
            }
            field.setAccessible(acc);
        }
    }

    public static  <T> void restore(final T instance, final Bundle state) {
        restore(instance, state, "");
    }

    public static <T> void restore(final T instance, final Bundle state, String instance_key) {
        Class<?> cls = instance.getClass();
        Field fields[] = cls.getDeclaredFields();
        for(Field field : fields) {
            String name = field.getName();
            if (!field.isAnnotationPresent(UI_SAVESTATE.class)) {
                //only work on UI_SAVESTATEs
                continue;
            }
            //search in bundle
            String key = cls.getCanonicalName()+"."+name;
            if (!instance_key.isEmpty()) {
                key = cls.getCanonicalName()+"["+instance_key+"]."+name;
            }
            if (state.containsKey(key)) {
                boolean acc = field.isAccessible();
                field.setAccessible(true);
                Class<?> typ = field.getType();
                if (Serializable.class.isAssignableFrom(typ)) {
                    try {
                        field.set(instance, state.getSerializable(key));
                    } catch (IllegalAccessException exp) {
                        //!TODO
                    }
                } else {
                    //!TODO
                }
                field.setAccessible(acc);
            }
        }
    }

    public static <T> View init(final T instance, final Bundle state) {
        return init(instance, state, "");
    }

    public static <T> View init(final T instance, final Bundle state, String instance_key) {
        Class<?> cls = instance.getClass();
        if (!cls.isAnnotationPresent(UI_LAYOUT.class)) {
            Log.e("UI", "No UI_LAYOUT set");
            return null;
        }

        //init layout
        UI_LAYOUT layout = cls.getAnnotation(UI_LAYOUT.class);
        View view = View.inflate(getContext(instance), layout.id(), null);

        //init all ui elements
        initUI(instance, view);

        //init stored state
        if (state != null) {
            restore(instance, state, instance_key);
        }

        return view;
    }

    public static  <T> void initUI(final T instance, final View view) {
        Class<?> cls = instance.getClass();
        Field fields[] = cls.getDeclaredFields();
        for(Field field : fields) {
            String name = field.getName();
            String onclick = null;
            if (field.isAnnotationPresent(UI_ELEMENT.class)) {
                UI_ELEMENT anno = field.getAnnotation(UI_ELEMENT.class);
                if (!anno.name().isEmpty()) {
                    name = anno.name();
                }
                if (!anno.onclick().isEmpty()) {
                    onclick = anno.onclick();
                }
            } else {
                //only work on UI_ELEMENTs
                continue;
            }
            //read
            int id = view.getResources().getIdentifier(name, "id", view.getContext().getPackageName());
            if (id == 0) {
                Log.e("UI", "Couldn't find ui resource " + field.getName() + "("+name+") by name");
                continue;
            }
            View ui_el = view.findViewById(id);
            if (ui_el == null) {
                Log.e("UI", "Couldn't find ui element " + field.getName() + "("+name+")  by id");
                continue;
            }
            boolean acc = field.isAccessible();
            field.setAccessible(true);
            try {
                field.set(instance, ui_el);
            } catch (IllegalAccessException exp) {
                Log.e("UI", "Couldn't set value for " + field.getName() +  "("+name+") " + exp.toString());
                continue;
            } finally {
                field.setAccessible(acc);
            }
            //set onclick
            if (onclick == null) {
                continue;
            }
            Method mth = null;
            try {
                mth = cls.getDeclaredMethod(onclick, View.class);
            } catch(NoSuchMethodException exp) {
                Log.e("UI", "Couldn't set onclick for " + field.getName() +  "("+name+") " + exp.toString());
                continue;
            }
            final Method mth_final = mth;
            final Field field_final = field;
            final String name_final = name;
            ui_el.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    long start = SystemClock.uptimeMillis();
                    boolean acc = mth_final.isAccessible();
                    mth_final.setAccessible(true);
                    try {
                        mth_final.invoke(instance, view);
                    } catch (IllegalAccessException exp) {
                        Log.e("UI", "Couldn't onclick for " + field_final.getName() +  "("+name_final+") " + exp.toString());
                    } catch (InvocationTargetException exp) {
                        Log.e("UI", "Couldn't onclick for " + field_final.getName() +  "("+name_final+") " + exp.toString());
                    } finally {
                        mth_final.setAccessible(acc);
                    }
                    long end = SystemClock.uptimeMillis();
                    long dif = end-start;
                    if (dif > TIME_WARNING_MS) {
                        Log.w("UI", "Slow onclick for " + field_final.getName() +  "("+name_final+") took " + dif + "ms");
                    }
                }
            });
        }
    }
}
