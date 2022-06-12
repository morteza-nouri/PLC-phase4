int g_var
bool flag
class A {
    private int x, y
    public initialize(int x, int y) {
        self.x = x
        self.y = y
        g_var = 1010
        print(g_var)
    }
}

class B {
    public void func() {
        print(g_var + 1)
        g_var = g_var + 1
    }
}

class Main {
    public initialize() {
        A a
        B b
        a = A.new(4, 6)
        b = B.new()
        b.func()
        print(g_var + 1)
    }
}

