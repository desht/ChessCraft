/**
 * Programmer: Jacob Scott
 * Program Name: ArrayManip
 * Description:
 * Date: Apr 18, 2011
 */
package com.jascotty2.util;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * @author jacob
 */
public class ArrayManip {

    public ArrayManip() {
    } // end default constructor

    public static void printArray(OutputStream out, int array[], int cols) throws IOException {
        //try {
        int maxLen = 1;
        for (int i = 0; i < array.length; ++i) {
            if (String.valueOf(array[i]).length() > maxLen) {
                maxLen = String.valueOf(array[i]).length();
            }
        }

        for (int i = 0; i < array.length; ++i) {
            out.write(Str.padLeft(String.valueOf(array[i]), maxLen + 1).getBytes());
            if (i % cols == cols - 1) {
                out.write("\n".getBytes());
            }
        }
        if ((array.length - 1) % cols != cols - 1) {
            out.write("\n".getBytes());
        }
        //} catch (IOException ex) {
        //    Logger.getAnonymousLogger().log(Level.SEVERE, ex.getMessage(), ex);
        //}
    }

    public static void printArray(OutputStream out, int array[]) throws IOException {
        for (int i = 0; i < array.length; ++i) {
            out.write(String.valueOf(array[i]).getBytes());
        }
    }

    public static void selectionSort(int array[], boolean ascending) {
        for (int i = 0; i < array.length - 1; ++i) {
            int swapI = i;
            for (int j = i + 1; j < array.length; ++j) {
                if ((ascending && array[j] < array[swapI])
                        || (!ascending && array[j] > array[swapI])) {
                    swapI = j;
                }
            }
            if (swapI != i) {
                int t = array[i];
                array[i] = array[swapI];
                array[swapI] = t;
            }
        }
    }

    public static void quickSort(int array[], boolean ascending) {
        Arrays.sort(array, 0, array.length);
        if (!ascending) {
            reverse(array);
        }
    }

    public static void reverse(int array[]) {
        for (int i = array.length / 2; i >= 0; --i) {
            int t = array[i];
            array[i] = array[array.length - i - 1];
            array[array.length - i - 1] = t;
        }
    }

    public static void swapElem(int array[], int a, int b) {
        if (a > 0 && b > 0 && a < array.length && b < array.length) {
            int t = array[a];
            array[a] = array[b];
            array[b] = t;
        }
    }

    public static int[] arrayConcat(int arr1[], int arr2[]) {
        if (arr1 == null || arr1.length == 0) {
            return arr2 == null ? new int[0] : arr2;
        } else if (arr2 == null || arr2.length == 0) {
            return arr1 == null ? new int[0] : arr1;
        }
        int i = 0;
        int ret[] = new int[arr1.length + arr2.length];
        for (; i < arr1.length; ++i) {
            ret[i] = arr1[i];
        }
        for (int n = 0; n < arr2.length; ++i, ++n) {
            ret[i] = arr2[n];
        }
        return ret;
    }

    public static <T> T[] arrayConcat(T arr1[], T arr2[]) {
        if (arr1 == null || arr1.length == 0) {
            return arr2 == null ? (T[]) new Object[0] : arr2;
        } else if (arr2 == null || arr2.length == 0) {
            return arr1 == null ? (T[]) new Object[0] : arr1;
        }
//        T ret[] = (T[]) new Object[arr1.length + arr2.length];
//        int i = 0;
//        for (; i < arr1.length; ++i) {
//            ret[i] = arr1[i];//Array.set(ret, i, arr1[i]);//
//        }
//        for (int n = 0; n < arr2.length; ++i, ++n) {
//            //System.out.println("saving " + n + " (" + arr2[n] + ")");
//            ret[i] = arr2[n];//Array.set(ret, i, arr2[n]);//
//        }
        T[] ret = (T[]) Array.newInstance(arr1.getClass().getComponentType(), arr1.length + arr2.length);
        System.arraycopy(arr1, 0, ret, 0, arr1.length);
        System.arraycopy(arr2, 0, ret, arr1.length - 1, arr2.length);
        return ret;
    }
} // end class ArrayManip

