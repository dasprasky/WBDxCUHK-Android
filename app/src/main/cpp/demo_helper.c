#include <ctype.h>
#include <string.h>

int g_int_array[10] = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

// Mutate array to uppercase
void uppercase(char* str) {
    size_t n = strlen(str);
    for (size_t i = 0; i < n; i++) {
        str[i] = toupper(str[i]);
    }
}

int squaredHelper(int n) {
    return n * n;
}
