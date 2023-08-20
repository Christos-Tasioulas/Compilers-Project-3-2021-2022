class Example {
    public static void main(String[] args) {
        int a;
        int b;
        int i; 
        int j;
        boolean c;
        boolean d;
        boolean e;
        int[] f;
        int g;
        int h;
        boolean k;

        a = 5;
        b = 2;
        a = a + b;
        System.out.println(a + b);

        i = 3;
        j = 5;
        c = i < j;
        d = !c;
        e = d && c;

        a = b + (i + j);
        System.out.println(a);

        

        a = 5;
        f = new int[5];
        f[3] = 5;

        

        k = true;

        if(k)
        {
            k = false;
        }
        else k = true;

        while(k)
        {
            k = false;
        }
    }
}
