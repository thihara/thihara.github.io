package com.company;// you can also use imports, for example:
// import java.util.*;

// you can write to stdout for debugging purposes, e.g.
// System.out.println("this is a debug message");

public class Main {
    public static int solution(int N) {

        int largestBGap = 0;
        int bGap = 0;
        boolean reached = false;
        while(N >= 1){
            int remainder = N % 2;

            if(remainder == 0 && N!=1){
                System.out.println("B : "+0);
                if(reached){
                    bGap++;
                }
            }else{
                System.out.println("B : "+1);
                if(reached){
                    reached = false;
                    largestBGap = largestBGap > bGap ? largestBGap : bGap;
                    bGap = 0;
                }
                reached = true;
            }

            if(N==1)
                break;

            N = N/2;
        }

        return largestBGap;
    }

    public static void main(String[] args) {
        System.out.println(solution(2147483647));
    }
}