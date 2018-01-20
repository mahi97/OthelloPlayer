import subprocess
from subprocess import PIPE, Popen


class Problem:
    def __init__(self):
        self.MLia = [10, 801.724, 382.026, 200.922, 74.396, 10]
        self.scores = {}

    def competency(self, state, clist):
        score = len(clist) + 1
        with open('param0.txt', 'w') as param:
            for s in state:
                param.write(str(s) + '\n')
            param.close()

            for l in clist:
                with open('param2.txt', 'w') as param:
                    if l is state:
                        l = self.MLia
                    for s in l:
                            param.write(str(s) + '\n')
                    param.close()

                with Popen(['java', 'game.Game', 'players.ParsianPlayer', 'players.ParsianPlayer'], stdout=PIPE,
                           stderr=PIPE) as proc:
                    for a in proc.stdout.readlines():
                        a = str(a)
                        a = a.replace(r'\r', '')
                        a = a.replace(r'\n', '')
                        a = a.replace(r'b', '')
                        if 'Player 2 won' in a:
                            if l is self.MLia:
                                print('Loosed to MLia')
                                score += 2
                            else:
                                print('Loosed')
                                score += 1
                        if 'Player 1 Won' in a:
                            if l is self.MLia:
                                print('Won from MLia')
                                score -= 2
                            else:
                                print('Won')
                                score -= 1
        return score

# a = Problem()
# a.competency([100, 100, 100, 100, 100, 100])
