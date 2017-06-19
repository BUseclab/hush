package just.trust.beri;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/**
 * Created by beri on 6/12/15.
 */
public class parser {

    //XC_MethodHook.MethodHookParam param
    /** Arguments to the method call */
    private String hookedFunction = "";
    private String packageName = "";
    private String method = "";
    private String methodName = "";
    private String methodClassName = "";
    private ArrayList<String> args = new ArrayList<String>();
    private ArrayList<String> keys = new ArrayList<String>();
    private Object result = null;

    public parser(String hookedCall, String packageName, XC_MethodHook.MethodHookParam param){
        if(param == null){
            return;
        }


        try {
            this.packageName = packageName;
            this.hookedFunction = hookedCall;
            this.method = param.method.toString();
            this.methodName= param.method.getName();
            this.methodClassName = param.method.getDeclaringClass().getName();

            if(this.hookedFunction.contains("org.json.JSONObject")) {
                org.json.JSONObject x = (org.json.JSONObject) param.thisObject;
                Iterator<String> iter = x.keys();
                while(iter.hasNext()) {
                        String a = iter.next();
                        this.keys.add(a);
                }
            }
            else if(this.hookedFunction.equals("org.json.JSONArray.constructor")) {
                org.json.JSONArray x = (org.json.JSONArray) param.thisObject;
                this.keys.add(x.toString());

            }


            for(Object a:param.args){
                if(a != null)
                    try {
                        this.args.add(a.getClass().toString()+ "#####" + a.toString());
                    }catch (Exception e){
                        continue;
                    }

            }


            if(this.methodName.contains("org.json.JSONObject")){
                try {
                    this.result = param.thisObject.toString();
                } catch (Exception e) {
                    this.result = "";
                }

            }

            /*if( param.getResult() == null) {
                try {
                    this.result = param.thisObject.toString();
                } catch (Exception e) {
                    this.result ="";
                }

            }else {
                try {
                    this.result = param.getResult().toString();
                } catch (Exception e) {
                    this.result = "";
                }
            }*/
        } catch (Exception e) {
            //e.printStackTrace();
            XposedBridge.log("#XposedLog:Error in parsing");
        }
    }
}
