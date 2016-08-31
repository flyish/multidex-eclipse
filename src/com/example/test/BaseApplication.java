package com.example.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;
import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

public class BaseApplication extends Application {
	
	@Override
	public void onCreate() {
		super.onCreate();
		Toast.makeText(this, "Application init...", Toast.LENGTH_SHORT).show();
		try {
			loadDex();
			injectDex();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//����dex�ְ�
	private void loadDex() throws Exception {
		File originFile = new File(getApplicationInfo().sourceDir); //��Ҫ������apk�ļ�
		File newAPK = new File(getFilesDir().getAbsolutePath() + File.separator + "app.apk"); // ��ȡ�������apk���·��
		if(!newAPK.exists()) {
			newAPK.createNewFile();
		}
		//����apk��˽��Ŀ¼
		copyFile(new FileInputStream(originFile), new FileOutputStream(newAPK));
		//��ѹapk�е�dex�ļ�
		ZipFile apk = new ZipFile(newAPK);
		Enumeration<? extends ZipEntry> en = apk.entries();
		ZipEntry zipEntry = null;
		while(en.hasMoreElements()) {
			zipEntry = (ZipEntry) en.nextElement();
			if(!zipEntry.isDirectory() && zipEntry.getName().endsWith("dex") && !"classes.dex".equals(zipEntry.getName())) {
				//����dex��destDir
				Log.e("yubo", "zip entry name: " + zipEntry.getName() + ", file size: " + zipEntry.getSize());
				InputStream inputStream = apk.getInputStream(zipEntry);
				FileOutputStream fout = openFileOutput(zipEntry.getName(), MODE_PRIVATE);
				copyFile(inputStream, fout);
			}
		}
		apk.close();
	}
	
	//��app_dexĿ¼��ȡ��dex�ļ���ע�뵽ϵͳ��PathClassLoader
	private void injectDex() {
		File[] files = getFilesDir().listFiles();
		if(files != null) {
			for(File f : files) {
				String fileName = f.getName();
				if(fileName.endsWith("dex") && !"classes.dex".equals(fileName)) {
					inject(f.getAbsolutePath());
					Log.e("yubo", "inject file: " + f.getAbsolutePath());
				}
			}
		}
	}
	
	//�����ļ�
	private void copyFile(InputStream is, FileOutputStream fos) {
		try {
			int hasRead = 0;
			byte[] buf = new byte[1024];
			while((hasRead = is.read(buf)) > 0) {
				fos.write(buf, 0, hasRead);
			}
			fos.flush();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (fos != null) {
					fos.close();
				}
				if(is != null) {
					is.close();
				}
			} catch (Exception e2) {
			}
		}
	}
	
	//ע��dex����libPathΪdex����·��
	public String inject(String libPath) {  
	    boolean hasBaseDexClassLoader = true;  
	    try {  
	        Class.forName("dalvik.system.BaseDexClassLoader");  
	    } catch (ClassNotFoundException e) {  
	        hasBaseDexClassLoader = false;  
	    }  
	    if (hasBaseDexClassLoader) {  
	        PathClassLoader pathClassLoader = (PathClassLoader)getClassLoader();  
	        DexClassLoader dexClassLoader = new DexClassLoader(libPath, getDir("dex", 0).getAbsolutePath(), libPath, getClassLoader());  
	        try {  
	            Object dexElements = combineArray(getDexElements(getPathList(pathClassLoader)), getDexElements(getPathList(dexClassLoader)));  
	            Object pathList = getPathList(pathClassLoader);  
	            setField(pathList, pathList.getClass(), "dexElements", dexElements);  
	            return "SUCCESS";  
	        } catch (Throwable e) {  
	            e.printStackTrace();  
	            return android.util.Log.getStackTraceString(e);  
	        }  
	    }  
	    return "SUCCESS";  
	}
	
	public Object getPathList(Object baseDexClassLoader)
            throws IllegalArgumentException, NoSuchFieldException, IllegalAccessException, ClassNotFoundException {
        return getField(baseDexClassLoader, Class.forName("dalvik.system.BaseDexClassLoader"), "pathList");
    }
	
	public Object getField(Object obj, Class<?> cl, String field)
            throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field localField = cl.getDeclaredField(field);
        localField.setAccessible(true);
        return localField.get(obj);
    }
	
	public static void setField(Object obj, Class<?> cl, String field, Object value)
            throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

        Field localField = cl.getDeclaredField(field);
        localField.setAccessible(true);
        localField.set(obj, value);
    }
	
	public Object getDexElements(Object paramObject)
            throws IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
        return getField(paramObject, paramObject.getClass(), "dexElements");
    }
	
	public static Object combineArray(Object arrayLhs, Object arrayRhs) {
        Class<?> localClass = arrayLhs.getClass().getComponentType();
        int i = Array.getLength(arrayLhs);
        int j = i + Array.getLength(arrayRhs);
        Object result = Array.newInstance(localClass, j);
        for (int k = 0; k < j; ++k) {
            if (k < i) {
                Array.set(result, k, Array.get(arrayLhs, k));
            } else {
                Array.set(result, k, Array.get(arrayRhs, k - i));
            }
        }
        return result;
    }
	
}
