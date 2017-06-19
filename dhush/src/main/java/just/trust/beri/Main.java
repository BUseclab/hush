package just.trust.beri;


import android.os.Environment;
import android.util.Pair;

import org.apache.commons.collections4.MultiMap;
import org.apache.commons.collections4.map.MultiValueMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.StringWriter;


import java.io.IOException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.Method;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


import static de.robv.android.xposed.XposedBridge.hookAllConstructors;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.XposedBridge;

import static de.robv.android.xposed.XposedBridge.hookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.findMethodBestMatch;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookConstructor;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findMethodExact;
import static de.robv.android.xposed.XposedHelpers.newInstance;

import com.google.gson.Gson;


public class Main implements IXposedHookLoadPackage {
    String packageName = "";
    //method to class map for hooking method only once
    MultiMap mcm = new MultiValueMap();
    Gson gson = new Gson();
    List<String> alreadyHooked = new ArrayList<String>();


    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        XposedBridge.log("xposed load: " + lpparam.packageName + "##########");
        String targetPackage = readPackageName();
        //if (!lpparam.packageName.equals(targetPackage))
        //      return;

        if (lpparam.packageName.contains("com.google")) {
            return;
        }

        //try to hook all
        this.hookAll(lpparam);

        try {
            hookAllConstructors(findClass("org.json.JSONObject", lpparam.classLoader), new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                    parser p = new parser("org.json.JSONObject.constructor", lpparam.packageName, param);
                    XposedBridge.log("#XposedLog:#" + gson.toJson(p, parser.class));

                }
            });
        } catch (XposedHelpers.ClassNotFoundError e) {

        } catch (NoSuchMethodError k) {

        }

        try {
            hookAllConstructors(findClass("org.json.JSONTokener", lpparam.classLoader), new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                    parser p = new parser("org.json.JSONTokener.constructor", lpparam.packageName, param);
                    XposedBridge.log("#XposedLog:#" + gson.toJson(p, parser.class));

                }
            });
        } catch (XposedHelpers.ClassNotFoundError e) {

        } catch (NoSuchMethodError k) {

        }


        try {
            hookAllConstructors(findClass("org.json.JSONArray", lpparam.classLoader), new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                    parser p = new parser("org.json.JSONArray.constructor", lpparam.packageName, param);
                    XposedBridge.log("#XposedLog:#" + gson.toJson(p, parser.class));

                }
            });
        } catch (XposedHelpers.ClassNotFoundError e) {

        } catch (NoSuchMethodError k) {

        }


        try {

            findAndHookMethod("org.json.JSONObject", lpparam.classLoader, "get", String.class, new XC_MethodHook() {


                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                    XposedBridge.log("#XposedLog:#" + lpparam.packageName +"#" + "org.json.JSONObject.get#arg:" + param.args[0].getClass() + "#" + param.args[0] + "#" + gson.toJson(param.getResult()) + "##########");
                    parser p = new parser("org.json.JSONObject.get_string", lpparam.packageName, param);
                    XposedBridge.log("#XposedLog:#" + gson.toJson(p, parser.class));

                }
            });
        } catch (XposedHelpers.ClassNotFoundError e) {
            //  XposedBridge.log("XposedLog: Exception 1");

        } catch (NoSuchMethodError k) {
            //   XposedBridge.log("XposedLog: Exception 1");
        }


        //Boolean
        try {

            findAndHookMethod("org.json.JSONObject", lpparam.classLoader, "getBoolean", Boolean.class, new XC_MethodHook() {


                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                    XposedBridge.log("#XposedLog:#" + lpparam.packageName +"#" + "org.json.JSONObject.getBoolean#arg:" + param.args[0].getClass() + "#" + param.args[0] + "#" + gson.toJson(param.getResult()) + "##########");
                    parser p = new parser("org.json.JSONObject.getBoolean", lpparam.packageName, param);
                    XposedBridge.log("#XposedLog:#" + gson.toJson(p, parser.class));

                }
            });
        } catch (XposedHelpers.ClassNotFoundError e) {
            // XposedBridge.log("XposedLog: Exception 1");

        } catch (NoSuchMethodError k) {
            //   XposedBridge.log("XposedLog: Exception 1");
        }


        //double
        try {

            findAndHookMethod("org.json.JSONObject", lpparam.classLoader, "getDouble", Double.class, new XC_MethodHook() {


                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                    XposedBridge.log("#XposedLog:#" + lpparam.packageName +"#" + "org.json.JSONObject.getDouble#arg:" + param.args[0].getClass() + "#" + param.args[0] + "#" + gson.toJson(param.getResult()) + "##########");
                    parser p = new parser("org.json.JSONObject.getDouble", lpparam.packageName, param);
                    XposedBridge.log("#XposedLog:#" + gson.toJson(p, parser.class));

                }
            });
        } catch (XposedHelpers.ClassNotFoundError e) {
            //XposedBridge.log("XposedLog: Exception 1");

        } catch (NoSuchMethodError k) {
            //   XposedBridge.log("XposedLog: Exception 1");
        }


        //int
        try {

            findAndHookMethod("org.json.JSONObject", lpparam.classLoader, "getInt", Double.class, new XC_MethodHook() {


                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                    XposedBridge.log("#XposedLog:#" + lpparam.packageName +"#" + "org.json.JSONObject.getInt" + "#arg#arg:" + param.args[0].getClass() + "#" + param.args[0] + "#" + gson.toJson(param.getResult()) + "##########");
                    parser p = new parser("org.json.JSONObject.getint", lpparam.packageName, param);
                    XposedBridge.log("#XposedLog:#" + gson.toJson(p, parser.class));

                }
            });
        } catch (XposedHelpers.ClassNotFoundError e) {
            //XposedBridge.log("XposedLog: Exception 1");

        } catch (NoSuchMethodError k) {
            //   XposedBridge.log("XposedLog: Exception 1");
        }


        //JSONArray
        try {

            findAndHookMethod("org.json.JSONObject", lpparam.classLoader, "getJSONArray", Double.class, new XC_MethodHook() {


                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                    //XposedBridge.log("#XposedLog:#" + lpparam.packageName +"#" + "org.json.JSONObject.getJSONArray#arg:" + param.args[0].getClass() + "#" + param.args[0] + "#" + gson.toJson(param.getResult()) + "##########");
                    parser p = new parser("org.json.JSONObject.getJSONArray", lpparam.packageName, param);
                    XposedBridge.log("#XposedLog:#" + gson.toJson(p, parser.class));

                }
            });
        } catch (XposedHelpers.ClassNotFoundError e) {
            //XposedBridge.log("XposedLog: Exception 1");

        } catch (NoSuchMethodError k) {
            //   XposedBridge.log("XposedLog: Exception 1");
        }


        //JSONObject
        try {

            findAndHookMethod("org.json.JSONObject", lpparam.classLoader, "getJSONObject", Double.class, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                    XposedBridge.log("#XposedLog:#" + lpparam.packageName +"#" + "org.json.JSONObject.getJSONObject#arg:" + param.args[0].getClass() + "#" + param.args[0] + "#" + gson.toJson(param.getResult()) + "##########");
                    parser p = new parser("org.json.JSONObject.getJSONObject_double", lpparam.packageName, param);
                    XposedBridge.log("#XposedLog:#" + gson.toJson(p, parser.class));
                }
            });
        } catch (XposedHelpers.ClassNotFoundError e) {
            //XposedBridge.log("XposedLog: Exception 1");

        } catch (NoSuchMethodError k) {
            //   XposedBridge.log("XposedLog: Exception 1");
        }


        //long
        try {

            findAndHookMethod("org.json.JSONObject", lpparam.classLoader, "getLong", Double.class, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                    XposedBridge.log("#XposedLog:#" + lpparam.packageName +"#" + "org.json.JSONObject.getLong#arg:" + param.args[0].getClass() + "#" + param.args[0] + "#" + gson.toJson(param.getResult()) + "##########");
                    parser p = new parser("org.json.JSONObject.getLong", lpparam.packageName, param);
                    XposedBridge.log("#XposedLog:#" + gson.toJson(p, parser.class));
                }
            });
        } catch (XposedHelpers.ClassNotFoundError e) {
            //XposedBridge.log("XposedLog: Exception 1");

        } catch (NoSuchMethodError k) {
            //   XposedBridge.log("XposedLog: Exception 1");
        }


        //string
        try {

            findAndHookMethod("org.json.JSONObject", lpparam.classLoader, "getString", Double.class, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                    XposedBridge.log("#XposedLog:#" + lpparam.packageName +"#" + "org.json.JSONObject.getString#arg:" + param.args[0].getClass() + "#" + param.args[0] + "#" + gson.toJson(param.getResult()) + "##########");
                    parser p = new parser("org.json.JSONObject.getString", lpparam.packageName, param);
                    XposedBridge.log("#XposedLog:#" + gson.toJson(p, parser.class));

                }
            });
        } catch (XposedHelpers.ClassNotFoundError e) {
            //XposedBridge.log("XposedLog: Exception 1");

        } catch (NoSuchMethodError k) {
            //   XposedBridge.log("XposedLog: Exception 1");
        }


        /*
        setText
         */

        try {
            Class<?> cTextView = findClass("android.widget.TextView", lpparam.classLoader);
            Method setText = findMethodBestMatch(cTextView, "setText", CharSequence.class);

            XposedBridge.hookMethod(setText, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    // XposedBridge.log("XposedLog: android.widget.TextView");
                    // this will be called before the clock was updated by the original method
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    // this will be called after the clock was updated by the original method
                    //XposedBridge.log("XposedLog: android.widget.TextView " + param.args[0]);
                    //XposedBridge.log("#XposedLog:" + "android.widget.TextViewy#arg:" + param.args[0].getClass() + "#" + param.args[0] + "#" + param.getResult() + "##########");
                    //XposedBridge.log("#XposedLog:#" + lpparam.packageName +"#" + "android.widget.TextView#arg:" + param.args[0].getClass() + "#" + param.args[0] + "##########");
                    parser p = new parser("android.widget.TextView.setText",lpparam.packageName, param);
                    XposedBridge.log("#XposedLog:#" + gson.toJson(p, parser.class));

                }
            });
        } catch (XposedHelpers.ClassNotFoundError e) {
            //XposedBridge.log("XposedLog: Exception 1");

        }



    /*
    GSON GOOGLE
     */
        try {


            findAndHookMethod("com.google.gson.Gson.GsonBuilder", lpparam.classLoader, "create", new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    parser p = new parser("com.google.gson.Gson.GsonBuilder.create", lpparam.packageName, param);
                    XposedBridge.log("#XposedLog:#" + gson.toJson(p, parser.class));

                }
            });
        } catch (XposedHelpers.ClassNotFoundError e) {
            //      XposedBridge.log("XposedLog: Exception Gson.create");

        }


        try {


            hookAllConstructors(findClass("com.google.gson.Gson", lpparam.classLoader), new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    parser p = new parser("com.google.gson.Gson.GsonBuilder.constructor", lpparam.packageName, param);
                    XposedBridge.log("#XposedLog:#" + gson.toJson(p, parser.class));

                }
            });
        } catch (XposedHelpers.ClassNotFoundError e) {
            //XposedBridge.log("XposedLog: Exception com.google.gson");

        }


        try {


            hookAllConstructors(findClass("com.google.gson.GsonBuilder", lpparam.classLoader), new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    parser p = new parser("com.google.gson.GsonBuilder", lpparam.packageName, param);
                    XposedBridge.log("#XposedLog:#" + gson.toJson(p, parser.class));

                    //XposedBridge.log("#XposedLog:#" + lpparam.packageName +"#" + "com.google.gson.GsonBuilder.constructor#arg:##" + param.thisObject + "#3##########");
                }
            });
        } catch (XposedHelpers.ClassNotFoundError e) {
            //XposedBridge.log("XposedLog: Exception GsonBuilder");

        }


        try {

            findAndHookMethod("com.google.gson.Gson", lpparam.classLoader, "fromJson", java.io.Reader.class, Class.class, new XC_MethodHook() {

                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    // this will be called before the clock was updated by the original method
                    Set<String> classes = ClassReflect.analyze(param.args[1].toString().replace("class ", ""), lpparam.classLoader);
                    for (String cl : classes) {
                        classMethodHooking(cl, lpparam, "");
                    }//for loop for hooking class

                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    //copy the reader
                    StringWriter writer = new StringWriter();
                    IOUtils.copy((InputStreamReader) param.args[0], writer);
                    String theString = writer.toString();

                    parser p = new parser("com.google.gson.Gson.fromString_1", lpparam.packageName, param);
                    XposedBridge.log("#XposedLog:#" + gson.toJson(p, parser.class));


                }
            });
        } catch (XposedHelpers.ClassNotFoundError e) {
            //  XposedBridge.log("XposedLog: Exception 1");

        } catch (NoSuchMethodError k) {
            //  XposedBridge.log("XposedLog: Exception 1");
        }


        try {

            findAndHookMethod("com.google.gson.Gson", lpparam.classLoader, "fromJson", java.io.Reader.class, Type.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                    Set<String> classes = ClassReflect.analyze(param.args[1].toString().replace("class ", ""), lpparam.classLoader);
                    for (String cl : classes) {
                        XposedBridge.log("dynamic hooking " + cl);
                        classMethodHooking(cl, lpparam, "");
                    }//for loop for hooking class

                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    parser p = new parser("com.google.gson.Gson.fromString_2", lpparam.packageName, param);
                    XposedBridge.log("#XposedLog:#" + gson.toJson(p, parser.class));

                }
            });
        } catch (XposedHelpers.ClassNotFoundError e) {
            // XposedBridge.log("XposedLog: Exception 2");

        }


        try {
            findAndHookMethod("com.google.gson.Gson", lpparam.classLoader, "fromJson", String.class, Class.class, new XC_MethodHook() {

                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                    Set<String> classes = ClassReflect.analyze(param.args[1].toString().replace("class ", ""), lpparam.classLoader);
                    for (String cl : classes) {
                        XposedBridge.log("dynamic hooking " + cl);
                        classMethodHooking(cl, lpparam, "");
                    }//for loop for hooking class

                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    // this will be called after the clock was updated by the original method
                    //XposedBridge.log("XposedLog: End Method 3");
                    //XposedBridge.log("#XposedLog:#" + lpparam.packageName +"#" + "com.google.gson.Gson.fromJson#arg:" + param.args[0].getClass() + ":" + param.args[0] + "#arg:" + param.args[1].getClass()
                    //        + ":" + param.args[1] + "#" + gson.toJson(param.getResult()) + "#6##########");
                    parser p = new parser("com.google.gson.Gson.fromString_3", lpparam.packageName, param);
                    try {
                        XposedBridge.log("#XposedLog:#" + gson.toJson(p, parser.class));
                    } catch (Exception e) {
                        //
                    }
                    //XposedBridge.log("-------------------------------------------");
                    //ClassSpy.spy(param.args[1].toString(), lpparam);
                    Set<String> classes = ClassReflect.analyze(param.args[1].toString().replace("class ", ""), lpparam.classLoader);
                    //for(String cl:classes) {
                        //XposedBridge.log("-------------------------------------------");
                        //classMethodHooking(cl, lpparam);
                        //XposedBridge.log("-------------------------------------------");
                    //}//for loop for hooking class

                }
            });
        } catch (XposedHelpers.ClassNotFoundError e) {
            //  XposedBridge.log("XposedLog: Exception 3");

        }

        try {
            findAndHookMethod("com.google.gson.Gson", lpparam.classLoader, "fromJson", String.class, Type.class, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    // this will be called after the clock was updated by the original method
                    // XposedBridge.log("#XposedLog:#" + lpparam.packageName +"#" + "com.google.gson.Gson.fromJson#arg:" + param.args[0].getClass() + ":" + param.args[0] + "#arg:" + param.args[1].getClass()
                    //         + ":" + param.args[1] + "#" + gson.toJson(param.getResult()) + "#7##########");
                    //classMethodHooking(param.args[1].toString(), lpparam);
                    parser p = new parser("com.google.gson.Gson.fromString_4", lpparam.packageName, param);
                    XposedBridge.log("#XposedLog:#" + gson.toJson(p, parser.class));
                }
            });
        } catch (XposedHelpers.ClassNotFoundError e) {
            // XposedBridge.log("XposedLog: Exception 4");

        }


        //This add to deob
        //////******************************
        //***********************************
        //***********************************

        List<Pair<String, String>> toHook = getHookedFunction();
        if (toHook == null)
            return;
        for (Pair<String, String> hook : toHook) {
            XposedBridge.log("XposedLog: hooking " + hook.first + "." + hook.second);
            String name = hook.first + "." + hook.second;
            if (!alreadyHooked.contains(name)) {
                XposedBridge.log("XposedLog: hooking effective " + hook.first + "." + hook.second);
                alreadyHooked.add(name);
                try {

                    findAndHookMethod(hook.first, lpparam.classLoader, hook.second, java.io.Reader.class, Class.class, new XC_MethodHook() {

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            //copy the reader
                            StringWriter writer = new StringWriter();
                            IOUtils.copy((InputStreamReader) param.args[0], writer);
                            String theString = writer.toString();

                            parser p = new parser("xxx.com.gson.Gson.fromString_Dynamic", lpparam.packageName, param);
                            XposedBridge.log("#XposedLog:#" + gson.toJson(p, parser.class));

                            Set<String> classes = ClassReflect.analyze(param.args[1].toString().replace("class ", ""), lpparam.classLoader);
                            for (String cl : classes) {
                                classMethodHooking(cl, lpparam, "");
                            }//for loop for hooking class

                        }
                    });
                } catch (XposedHelpers.ClassNotFoundError e) {
                    //  XposedBridge.log("XposedLog: Exception 1");

                } catch (NoSuchMethodError k) {
                    //  XposedBridge.log("XposedLog: Exception 1");
                }


                try {
                    findAndHookMethod(hook.first, lpparam.classLoader, hook.second, String.class, Class.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            // this will be called after the clock was updated by the original method
                            //XposedBridge.log("XposedLog: End Method 3");
                            //XposedBridge.log("#XposedLog:#" + lpparam.packageName +"#" + "com.google.gson.Gson.fromJson#arg:" + param.args[0].getClass() + ":" + param.args[0] + "#arg:" + param.args[1].getClass()
                            //        + ":" + param.args[1] + "#" + gson.toJson(param.getResult()) + "#6##########");
                            parser p = new parser("com.google.gson.Gson.fromString_Dynamic", lpparam.packageName, param);
                            XposedBridge.log("#XposedLog:#" + gson.toJson(p, parser.class));
                            //XposedBridge.log("-------------------------------------------");
                            //ClassSpy.spy(param.args[1].toString(), lpparam);
                            Set<String> classes = ClassReflect.analyze(param.args[1].toString().replace("class ", ""), lpparam.classLoader);
                            for (String cl : classes) {
                                //XposedBridge.log("-------------------------------------------");
                                classMethodHooking(cl, lpparam, "");
                                //XposedBridge.log("-------------------------------------------");
                            }//for loop for hooking class

                        }
                    });
                } catch (XposedHelpers.ClassNotFoundError e) {
                    //  XposedBridge.log("XposedLog: Exception 3");

                }

            }//if not hooked

        }//For loop

        ///end deob
        /**************************************************
         *
         * *************************************************
         ***************************************************/



    } //Handle loadPackage


    //other methods
    protected void classMethodHooking(String sClass, final LoadPackageParam lppram, final String prefix) {
        //XposedBridge.log("start classMethodHooking "+ sClass);
        Class classToInvestigate = null;
        String packageName = lppram.packageName;
        try {
           // if (packageName == null || packageName == "") {
           //     packageName = readPackageName();
           // }

            classToInvestigate = findClass(sClass.replace("class ", ""), lppram.classLoader);
            if (sClass.startsWith("[")){

                if(classToInvestigate.getCanonicalName().contains("[]")){
                    try {
                        classToInvestigate = Class.forName(classToInvestigate.getComponentType().toString().replace("class ", ""), false, lppram.classLoader);
                    }catch (NoClassDefFoundError e){
                        XposedBridge.log("No class found !!");
                    }
                }
                }


                if (packageName == null || classToInvestigate == null || lppram.packageName.contains("com.android") || lppram.packageName.contains("com.google")) {
                return;
            }

            XposedBridge.log("BeriDebug: " + classToInvestigate.getName());
            //check if the method has already been hooked
            //Very simple way we should implement a more specific one ==> treating method rather than class
            if (mcm.containsKey(classToInvestigate.getName())) {
                XposedBridge.log("XposedLog: Already Hooked");
                XposedBridge.log("start classMethodConstructorHooking 0 " + classToInvestigate.getName());
                return;
            }
            XposedBridge.log("start classMethodConstructorHooking 1 " + classToInvestigate.getName());
            mcm.put(classToInvestigate.getName(), "");
            XposedBridge.log("start classMethodConstructorHooking 2 " + classToInvestigate.getName());
            //Hook the constructor
            try {
                String classNameTmp = classToInvestigate.getName(); //.replace("$",".");
                XposedBridge.log("***XposedLog: classMethodConstructorHooking " + classNameTmp);
                hookAllConstructors(findClass(classNameTmp, lppram.classLoader), new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("***XposedLog: classMethodConstructorHookingEnter: " + lppram.packageName);
                        parser p = new parser(prefix.concat(" dynamic_hook.constructor"), lppram.packageName, param);
                        XposedBridge.log("#XposedLog:#" + gson.toJson(p, parser.class));

                    }
                });
            } catch (XposedHelpers.ClassNotFoundError e) {
                XposedBridge.log("Errorx: " + e.toString());

            } catch (NoSuchMethodError k) {
                XposedBridge.log("Errorxx: " + k.toString());
            }
            XposedBridge.log("start classMethodConstructorHooking 3 " + classToInvestigate.getName());

            Method[] aClassMethods = classToInvestigate.getDeclaredMethods();
            //XposedBridge.log("start classMethodHooking " + aClassMethods);
            for (Method m : aClassMethods) {
                // Found a method m
                XposedBridge.log("***XposedLog: classMethodHooking " + classToInvestigate.getName() + " => " + m.getName() + " " + Modifier.isPublic(m.getModifiers()));
                //if(m.getName().startsWith("get")) {
                //XposedBridge.log("XposedLog: Hooked");
                try {
                    hookMethod(m, new XC_MethodHook() {
                        //findAndHookMethod("com.google.gson.Gson", lpparam.classLoader, "fromJson", String.class, Type.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                            parser p = new parser(prefix.concat("XC_MethodHook"), lppram.packageName, param);
                            XposedBridge.log("#XposedLog:#" + gson.toJson(p, parser.class));

                        }
                    });
                } catch (XposedHelpers.ClassNotFoundError e) {
                    XposedBridge.log("XposedLog: Hook internal");

                }
                //} //if method starts with get ==> hook


            } //loop over all methods
            // } // if this is the package of the app

        } catch (SecurityException e) {
            // Access denied!
            XposedBridge.log("Errorxxx: " + e.toString());
        } catch (NoClassDefFoundError e) {
            XposedBridge.log("Errorxxxx: " + e.toString());
        } catch (Exception e) {
            // Unknown exception
            XposedBridge.log("Errorxxxxx: " + e.toString());
        }

    }


    private String readPackageName() {
        File sdcard = Environment.getExternalStorageDirectory();

        //Get the text file
        File file = new File(sdcard, "package.log");

        //Read text from file
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);

            }
            br.close();
            return text.toString();
        } catch (IOException e) {
            //You'll need to add proper error handling here
            return null;
        }

    }

    private List<Pair<String, String>> getHookedFunction() {
        File sdcard = Environment.getExternalStorageDirectory();

        //Get the text file
        File file = new File(sdcard, "hooked.log");

        //Read text from file
        StringBuilder text = new StringBuilder();

        List<Pair<String, String>> rst = new ArrayList<Pair<String, String>>();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                Pair<String, String> tmp = new Pair<String, String>(line.split("##")[0], line.split("##")[1]);
                XposedBridge.log("XposedLog: Add_hooking " + tmp.first + "." + tmp.second + "##");
                rst.add(tmp);
            }
            br.close();
            return rst;
        } catch (IOException e) {
            //You'll need to add proper error handling here
            return null;
        }

    }


    private void hookAll(LoadPackageParam lpparam) {
        File sdcard = Environment.getExternalStorageDirectory();

        //Get the text file
        File file = new File(sdcard, "hooks.log");

        //Read text from file
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                classMethodHooking(line, lpparam, "NF");

            }
            br.close();

        } catch (IOException e) {
            //You'll need to add proper error handling here

        }

    }


}//Main Class


