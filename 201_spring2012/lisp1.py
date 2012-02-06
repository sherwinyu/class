def cdr(x):
  return x[1:]

def car(x):
  return x[0]

def pieces(x):
  return f(x, str(x))

def f(x, path):
  if type(x) == list:
    if len(x) == 0:
      return str(x) + " " + path
    else:
      return cons(str(x) + " " + path, cons( f(car(x), "car "+path), f(cdr(x), "cdr "+path)) )
  else:
    return str(x) + " " + path

def cons(a, b):
  return a + "\n" + b

def scons(a, b):
  print ""

# a and b are lists
def append(a, b): 
  if type(a) != list or type(b) != list:
      raise TypeError("A and B have to be lists")
  return a + b



l = [1, 2, 3, [6, [7,8], 9], [4, 5] ]

#append(3,[4,5,6])
print pieces(l)
