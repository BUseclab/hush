package just.trust.beri;

/**
 * Created by beri on 6/9/15.
 */
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import de.robv.android.xposed.XposedBridge;


public class ClassReflect  {



    public static Set<String>  analyze(String sclass, ClassLoader cl) {


        //This queue is for getting all the data
        Queue<String> queueA = new LinkedList();
        //the final result is here
        Set<String> rst = new HashSet<String>();

        queueA.add(sclass);
        while(!queueA.isEmpty()){
            String classInvest = (String) queueA.poll();
            //XposedBridge.log("Class to analyse: " + classInvest);
            //class done?
            if(rst.contains(classInvest)){
              //  XposedBridge.log("Class not added: " + classInvest);
                continue;
            }
            //else ==> investigate
            Set<String> miniRst= classDeconstruct(classInvest, cl);
            //Mark the current class as done
            //XposedBridge.log("Class added: " + classInvest);
            rst.add(classInvest);
            //miniRst is null
            if(miniRst == null)
                continue;
            //set the class to analyze in the queue
            for(String r:miniRst){
                if(!rst.contains(r))
                    queueA.add(r);
            }

        }//Queue

        XposedBridge.log("Analyze " + rst.toString());
        return rst;
    }//main





    public static Set<String> classDeconstruct(String className, ClassLoader clsLoader){
        try {
            Set<String> rst = new HashSet<String>();

            Class<?> c = null;
            try{
                c = Class.forName(className, false, clsLoader);

                if (className.startsWith("[")){
                    XposedBridge.log("Type +" + getParameterizedTypes(c)+ " " + c.getCanonicalName() + " " + c.getComponentType());
                    if(c.getCanonicalName().contains("[]")){
                        c = Class.forName(c.getComponentType().toString().replace("class ",""), false, clsLoader);
                    }


                }
            }catch(ClassNotFoundException e){
                //System.err.println("Class not found " + className);
            }
            if(c==null || c.isInterface()){
                return null;
            }

            XposedBridge.log("Class Name " + c.getName());
            //Check if it extend someclass
            if(! isPrimitive(c.getSuperclass()))
                rst.add(c.getSuperclass().getName());

            //Then  decomposing Fields
            for(Field f: c.getDeclaredFields()){
                ArrayList<String> r = deconstructField(f);
                //is not primitive and have not been added than add
                for(String ff:r){
                    if(!ff.equals("") && !rst.contains(ff))
                        rst.add(ff);
                }
            }
            //then decomposing methods
            for(Method m:c.getMethods()){
                XposedBridge.log("method: " + m.getName());
                ArrayList<String> r = deconstructMethod(m);
                rst.addAll(r);

            }

            XposedBridge.log("classDeconstruct" + rst.toString());
            return rst;

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;

    }

    private static ArrayList<String> deconstructField(Field f) {
        // Given a field check whether is primitive or not
        ArrayList<String> rst = new ArrayList<String>();
        //System.out.println("\t\tField : " + f.getName() + " " + f.getType() + "==>" + f.getType().isInterface());
        //interface

        Type type = f.getGenericType();
        if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType)type;
           // System.out.print("Raw type: " + getTypeName(pType.getRawType()) + " - ");
            if(!isPrimitive(pType.getRawType()))
                rst.add( getTypeName(pType.getRawType()));
            for(Type arg:pType.getActualTypeArguments()){
               // System.out.println("Type args: " + getTypeName(arg));
                if(!isPrimitive(arg))
                    rst.add(getTypeName(arg));
            }
        }
        else {
           // System.out.println("\t\tType: " + getTypeName(f.getType()));
            if(!isPrimitive(f.getType()))
                rst.add(trim(getTypeName(f.getType())));
        }



        XposedBridge.log("\t\tRst deconstructField:"+rst.toString());
        return rst;
    }

    public static Type[] getParameterizedTypes(Object object) {
        Type superclassType = object.getClass().getGenericSuperclass();
        if (!ParameterizedType.class.isAssignableFrom(superclassType.getClass())) {
            return null;
        }
        return ((ParameterizedType)superclassType).getActualTypeArguments();
    }


    //Given a method check params and return value
    public static ArrayList deconstructMethod(Method m){
        ArrayList<String> rst = new ArrayList<String>();
        //XposedBridge.log("\t Method: " + m.getName());
        //start with the parameters
        for(Type t:m.getGenericParameterTypes()){
            //XposedBridge.log("\t\tdeconstructMethod Param Type: " + t.toString().replace("class ", "") + " " + t.toString().replace("class ", "") + " " + isPrimitive(t));
            if(!isPrimitive(t))
                if(!rst.contains(t.toString().replace("class ","")))
                    rst.add(t.toString().replace("class ",""));
        }

        //return value
        //XposedBridge.log("\t\tdeconstructMethod ret Type: " + m.getReturnType() + " " + isPrimitive(m.getReturnType()));
        if(!isPrimitive(m.getReturnType()))
            if(! rst.contains(m.getReturnType().getName()))
                rst.add(trim(m.getReturnType().getName()));

        //ret
        XposedBridge.log("\t\tRst deconstructMethod:" + rst);
        return rst;
    }


    public static String trim(String x){
        if(x.startsWith("[")){
            //[Lcom.united.mobile.models.MOBTypeOption;
            x = x.replace("[L","");
            x = x.replace(";","");
        }
        return x;
    }

    public static boolean isPrimitive(Type t){
        //all complex method belong to package so xx.yy.ww
        if(t == null){
            return true;
        }
		/*if(!t.getTypeName().startsWith("[")){
			return true;
		}*/

        if(!getTypeName(t).contains(".")){
            return true;
        }

        if(t.getClass().isPrimitive())
            return true;
        if(getTypeName(t).startsWith("android")){
            return true;
        }
        if(getTypeName(t).startsWith("java.")){
            //System.out.println(t.getClass().getName());
            return true;
        }
        return false;
    }

    private static String getTypeName(Type T){
        return T.toString().replace("class ","").replace("interface ","");
    }
}