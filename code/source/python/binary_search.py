# A log(n) time implementation of binary search through a sorted list.

def binary_search(list_, val, start, end):
    if val >= list_[(start+end)/2] and val < list_[(start+end)/2+1]:
        return (start+end)/2
    elif val < list_[(start+end)/2]:
        return binary_search(list_, val, start, (start+end)/2)
    else:
        return binary_search(list_, val, (start+end)/2+1, end)
