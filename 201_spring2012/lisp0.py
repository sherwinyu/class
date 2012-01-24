def fun_to_str(x):
  if x==cdr:
    return 'cdr'
  if x==car:
    return 'car'
  return str(x)

def cdr(x):
  return x[1:]

def car(x):
  return x[0]

def cons(a, b):
  return [a, b]

# precondition: a and b are lists
def append(a, b):
  if type(a) != list or type(b) != list:
      raise TypeError("A and B have to be lists: a=" + str(a) + "\tb=" + str(b) + str(type(a)) + str(type(b)))
  return a + b

def pieces(x):
  return f(x, [x])





# path is a list
# path f returns a list of tuples of the form
# [ (val, path) ]
# where applying path to x yields val
def f(x, path):
  if type(x) == list:
    if len(x) == 0:
      return [cons(x, path)]
    else:
      return append( append( [cons(x, path)], f(car(x), append( [car], path))) ,   f(cdr(x), append([cdr], path)) )
  else:
    return [cons(x, path)]

l = [1, 2, [4,5], [4,5,6]]

p = pieces(l)
print p
print "\n prettyprint:"
#print "\n".join(map(lambda : str(ele) + "<==>" + " ".join(path), p))
for (ele, path) in p:
  print "%s <===> %s" %(ele, " ".join(map(fun_to_str, path)))
  print "\t\t verification: " + str(reduce(lambda a, x: x(a), reversed(path[:-1]), path[-1]))

