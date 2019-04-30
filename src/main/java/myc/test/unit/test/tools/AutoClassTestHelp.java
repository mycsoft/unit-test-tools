package myc.test.unit.test.tools;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.fail;

/**
 * 自动测试类. 本类对于指定的范围内的所有类的get/set,toString等方法进行配对测试. 指定测试路径,设置需要忽略的类与方法.
 * get/set必须成对测试. 测试原理 get=set.
 *
 * @author MaYichao
 */
public class AutoClassTestHelp {

    private static final String CLASS_XX = ".class";
    
    private static final Date DEFAULT_DATE = new Date();
    
    /**
     * 忽略类数组.
     */
    String[] IGNORES = { //        "cn.howso.interceptor.UrlRealm",
    //        "cn.howso.util.company.PaginationBean",
    //        "cn.howso.util.FtpProcess",
    //        "cn.howso.util.quartz.SchedulerConfig",
    //        "cn.howso.model.CategoryInfo"
    };
    int testedCount = 0;
    /**
     * 忽略列表.
     */
    private List<String> ignoreList = new ArrayList<>();
    /**
     * 错误列表.
     */
    private List<String> errList;

    public static AutoClassTestHelp create() {
        return create(null);
    }

    public static AutoClassTestHelp create(List<String> ignores) {
        AutoClassTestHelp help = new AutoClassTestHelp();
        help.ignoreList = ignores;
        return help;
    }

//    /**
//     * 对象创建器map.
//     */
//    private final Map<Class, Method> createrMap = new HashMap<>();
    private void initHelp() {
//        ignoreList = Arrays.asList(IGNORES);
        if (ignoreList == null) {
            ignoreList = new ArrayList<>();
        }
        errList = new ArrayList<>();

//        解析本类所有的对象创建方法.
//        initAllCreater();
    }
//
//    /**
//     * 扫描本类中所有的创建器方法.
//     */
//    private void initAllCreater() {
////        Class c = AutoClassTestHelp.class;
////        Method[] ms = c.getDeclaredMethods();
////        for (Method m : ms) {
////            InstanceCreater ic = m.getAnnotation(InstanceCreater.class);
////            if (ic != null) {
////                Class cls = ic.targetClass();
////                if (cls != null) {
////                    createrMap.put(cls, m);
////                }
////            }
////        }
//    }

    /**
     * 执行测试.
     *
     * 默认根路径为当前项目的classes目录.
     *
     * @throws ClassNotFoundException
     */
    public void startTest() throws Exception {

        //指定测试路径,设置需要忽略的类与方法.
        String rootPath = "../classes/";
        String target = "";
        startTest(rootPath, target);
        //get/set必须成对测试.
        //测试原理 get=set.
    }

    /**
     * 执行测试.
     *
     * @param rootPath 根据路径
     * @param target 子路径或文件.
     * @throws ClassNotFoundException
     */
    public void startTest(String rootPath, String target) throws ClassNotFoundException {
        try {
            initHelp();
            //扫描出所有的class文件.
            File root = new File(getClass().getResource("/").getFile(), rootPath);
            File targetFile = new File(root, target);
            scan(targetFile, root.getPath());
        } finally {
            //打印结果.
            String s = String.format("Get/Set测试%s,共测试%d条记录,发生%d处错误.具体请查看明细:",
                    errList.isEmpty() ? "成功" : "失败", testedCount, errList.size());
            System.out.println(s);
            if (!errList.isEmpty()) {
                //有错误,显示失败信息,返回失败.
                System.out.println("=========== 错误明细 ==============");
                errList.forEach((errLog) -> {
                    System.out.println(errLog);
                });
                fail(s);
            }
        }
    }

    /**
     * 扫描一个目录.
     *
     * @param dir 目录
     * @param rootPath 根目录,顶级包所在的位置.用于分析包名.
     */
    private void scan(File dir, String rootPath) throws ClassNotFoundException {
        if (dir == null || !dir.exists()) {
            throw new NullPointerException("Can't scan null dir!");
        }
        if (!dir.isDirectory()) {
            throw new IllegalStateException("Only scan dir!");
        }

        File[] children = dir.listFiles();

        for (File child : children) {
            if (child.isDirectory()) {
                //下级目录
                scan(child, rootPath);
                continue;
            }

            if (child.isFile()) {
                //文件.
                String name = child.getName();
                if (name.endsWith(CLASS_XX)) {
                    //进行class处理.
                    //取出类名.
                    String className = parseClassName(child, rootPath);

                    if (ignoreList.contains(className)) {
                        //在忽略列表中.跳过.
                        continue;
                    }
                    try {
                        //得到类.
                        Class cls = Class.forName(className);
                        if (!Modifier.isAbstract(cls.getModifiers())) {
                            //不是抽象类.
                            //进行具体的类解析.
                            testClass(cls);
                        }

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        errList.add(String.format("%s类的测试发生未知异常:%s", className, ex.getLocalizedMessage()));

                    }
                }
            }
        }

    }

    /**
     * 解析出类的全名.
     *
     * @param file 类文件对象
     * @param rootPath 根目录,顶级包所在的位置.用于分析包名.
     * @return
     */
    private String parseClassName(File file, String rootPath) {
        String name = file.getName();

        if (name.endsWith(CLASS_XX)) {
            String path = file.getPath();
            if (!path.startsWith(rootPath)) {
                throw new RuntimeException(String.format("The file [%s] isn't child of dir[%s]", path, rootPath));
            }

            StringBuilder pkg = new StringBuilder(path.substring(rootPath.length()));
            pkg.delete(pkg.length() - 6, pkg.length());
            String p = pkg.toString();
            char d = File.separatorChar;
            if (pkg.length() > 0) {
                //修正首位.
                if (pkg.charAt(0) == d) {
                    pkg.deleteCharAt(0);
                }
                //分隔符转为'.'
                p = pkg.toString().replace(d, '.');
            }

            return p;

        } else {
            return null;
        }

    }

    /**
     * 测试一个具体的class
     *
     * @param cls
     */
    private void testClass(Class cls) {
        testAllGetSetMethod(cls);
        //扫描并测试出所有的内部类.
        testInneClass(cls);
        testToStringMethod(cls);

    }

    /**
     * 测试一个具体的class
     *
     * @param cls
     */
    private void testAllGetSetMethod(Class cls) {

        Method[] methods = cls.getMethods();
        Map<String, Method> setMap = new HashMap<>();
        Map<String, Method> getMap = new HashMap<>();
        //扫描出所有get/set方法.
        for (Method method : methods) {
            String name = method.getName();
            if (name.startsWith("set")) {
                setMap.put(name.substring(3), method);
            } else if (name.startsWith("get")) {
                getMap.put(name.substring(3), method);
            }
        }

        if (setMap.isEmpty() || getMap.isEmpty()) {
            //没有方法需要进行测试.
            return;
        }

        //创建实例,这是执行测试的前提.
        Object instance = createDefault(cls);

        if (instance == null) {
            //不能生成实例,不可以进行测试.
            return;
        }

        for (String property : setMap.keySet()) {
            Method set = setMap.get(property);
            Method get = getMap.get(property);
            testGetSetMethod(instance, get, set);
        }

    }

    /**
     * 测试一个具体的class的toString方法.
     *
     * @param cls
     */
    private void testToStringMethod(Class cls) {
        //创建实例,这是执行测试的前提.
        Object instance = createDefault(cls);

        if (instance != null) {
            instance.toString();
        }
    }

    /**
     * 创建一个实例.
     *
     * @param cls
     * @return
     */
    private Object createDefault(Class cls) {
        //创建实例,这是执行测试的前提.
        Object instance = null;
        //枚举型的构造方式.
        if (cls.isEnum()) {
            instance = getEnumDefault(cls);
        } else {
            //使用空构造器生成.
//            Method creater = createrMap.get(cls);
            Method creater = null;
            if (creater != null) {
                try {
                    instance = creater.invoke(null);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {

                Constructor[] cs = cls.getConstructors();

                for (Constructor c : cs) {
                    if (c.getParameterTypes().length != 0) {
                        //不支持多参数的构造器.
                        continue;
                    }
                    try {
                        instance = c.newInstance();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        errList.add(String.format("%s类的实例化%s", instance.getClass().getName(), c.getName()));
                    }
                }
            }
        }
        return instance;
    }

    /**
     * @param instance 实例对象.
     * @param get
     * @param set
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    private void testGetSetMethod(Object instance, Method get, Method set) {
        if (get == null || set == null) {
            //没有对应的get方法.
            return;
        }

        //判别参数类别(只支持基本类型.自定义类型需要扩展)
        if (set.getParameterTypes().length != 1) {
            //多个参数,不支持自定义.
            return;
        }

//        Parameter param = set.getParameters()[0];
        Class re = get.getReturnType();
//            String type = param.getParameterizedType().getTypeName();
        Class type = set.getParameterTypes()[0];

//            if ("int".equals(type)) {
//            } else if ("float".equals(type)) {
//            } else if ("double".equals(type)) {
//            } else if ("char".equals(type)) {
//            } else if ("boolean".equals(type)) {
//            } else if ("java.lang.String".equals(type)) {
//            } else if ("java.lang.Integer".equals(type)) {
//            } else if ("java.lang.Float".equals(type)) {
//            } else if ("java.lang.String".equals(type)) {
//            } else if ("java.lang.Double".equals(type)) {
//            } else if ("java.lang.Char".equals(type)) {
//            } else if ("java.lang.Boolean".equals(type)) {
//
//            }
        //get/set类型不对应.
        if (!type.equals(re)) {
            return;
        }
        Object o = null;
        if (int.class.equals(type)) {
            o = 123;
        } else if (float.class.equals(type)) {
            o = (float) 123.33;
        } else if (double.class.equals(type)) {
            o = 123.33;
        } else if (char.class.equals(type)) {
            o = '#';
        } else if (boolean.class.equals(type)) {
            o = Boolean.TRUE;
        } else if (String.class.equals(type)) {
            o = "1";
        } else if (Integer.class.equals(type)) {
            o = 123;
        } else if (Float.class.equals(type)) {
            o = (float) 123.33;
        } else if (Double.class.equals(type)) {
            o = 123.33;
        } else if (Character.class.equals(type)) {
            o = '#';
        } else if (Boolean.class.equals(type)) {
            o = Boolean.TRUE;
        } else if (Date.class.equals(type)) {
            o = DEFAULT_DATE;
        } else if (List.class.equals(type)) {
            o = new ArrayList();
        } else if (type.isEnum()) {
            //枚举型.
            o = getEnumDefault(type);
        } else {
            return;
        }

        //测试调用,确认是否get==set?
        String msg = String.format("%s类的%s/%s测试", instance.getClass().getName(), get.getName(), set.getName());
        try {
            set.invoke(instance, o);
            Object o2 = get.invoke(instance);
            if (!o.equals(o2)) {
                //发现不一样,加入到错误列表中.
                errList.add(String.format("%s失败,set的是<%s>,get的是<%s>", msg, String.valueOf(o), String.valueOf(o2)));
            }
            testedCount++;
        } catch (Exception ex) {
            ex.printStackTrace();
            errList.add(String.format("%s发生异常%s", msg, ex.getLocalizedMessage()));
//            fail(msg + "失败!请查看日志.");
        }

    }

    /**
     * 取得枚举类型的默认值.
     *
     * @param <E>
     * @param enumClass
     * @return
     */
    private static <E extends Enum> E getEnumDefault(Class<E> enumClass) {
        Object[] cs = enumClass.getEnumConstants();
        if (cs != null && cs.length > 0) {
            return (E) cs[0];
        }
        return null;
    }

    /**
     * 扫描并检查内部类.
     *
     * @param cls
     */
    private void testInneClass(Class cls) {
        //取出内部类.
        Class[] inneClasses = cls.getDeclaredClasses();
        if (inneClasses != null) {
            for (Class inneClass : inneClasses) {
                testClass(inneClass);
            }
        }
    }

//    @InstanceCreater(targetClass = CoSessionHelper.class)
//    private static Object createCoSessionHelper() {
//        MockHttpServletRequest request = new MockHttpServletRequest();
//        request.setSession(new MockHttpSession());
//        CoSessionHelper coSessionHelper = new CoSessionHelper(request);
//        return coSessionHelper;
//    }
//    @InstanceCreater(targetClass = Date.class)
//    private static Object createDate() {
//        return new Date();
//    }
}
