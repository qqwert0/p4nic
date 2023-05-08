import random

N = 128
Range = 1000


IntsPerLine = 16

def get_random_list(N,Range):
	l = random.sample(range(0,Range),N)
	l.sort()
	return l
	
l = get_random_list(N,Range)

l = [hex(n)[2:].zfill(8) for n in l]

final_l = []
for i in range(len(l)//IntsPerLine):
	s = ''
	for j in range(IntsPerLine):
		s = l[i*IntsPerLine+j]+s
	final_l.append(s)

f = open("data.txt","w")
for s in final_l:
	f.write(s+"\n")