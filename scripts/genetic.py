import problem
import random


class Genetic:
    def __init__(self):
        self.chromosomes = 10
        self.DNACount = 6
        self.Maxgeneration = 30
        self.currentGen = 0
        self.mutationRate = .4
        self.crossoverRate = .3
        self.chromosomesList = []
        self.fitness = []
        self.prob = []
        self.problem = problem.Problem()
        self.ans = []
        self.bestG = []
        self.meanG = []
        self.worstG = []

    def generate_chromosome(self):
        for _ in range(self.chromosomes):
            self.chromosomesList.append(random.sample(range(0, 1000), self.DNACount))

    def evaluation_fitness(self):
        self.currentGen += 1
        fitness = []
        for c in self.chromosomesList:
            print("COEF: ", c)
            fn = self.problem.competency(c, self.chromosomesList)
            fitness.append(fn)
            print("WIN:", len(self.chromosomesList) - fn + 1)
        self.fitness.clear()
        for f in fitness:
            self.fitness.append(1 / (1 + f))

    def select_chromosome(self):
        print("Guys: ", self.chromosomesList)
        print("Fitness:", self.fitness)
        total = sum(self.fitness)
        cumulative = []
        self.prob.clear()
        for f in self.fitness:
            self.prob.append(f / total)
            cumulative.append(sum(self.prob))

        best = 0
        bestid = 0
        for i in range(len(self.prob)):
            if self.prob[i] > best:
                best = self.prob[i]
                bestid = i

        newChromosomesList = []
        newChromosomesList = [self.chromosomesList[bestid][:]]
        for _ in range(self.chromosomes):
            rand = random.uniform(0, 1)
            count = 0
            for c in cumulative:
                if rand < c:
                    newChromosomesList.append(self.chromosomesList[count][:])
                    break
                else:
                    count += 1
        self.chromosomesList = newChromosomesList
        # print(self.chromosomesList)

    def cross_over(self):
        parent = []
        for i in range(self.chromosomes):
            if random.random() < self.crossoverRate:
                if self.chromosomesList[i] not in parent:
                    parent.append(self.chromosomesList[i][:])
        child = []
        if len(parent) < 2:
            return
        for i in range(len(parent)):
            for j in range(i, len(parent)):
                if i is not j:
                    r = random.randint(1, self.DNACount - 1)
                    c = parent[i][:r] + parent[j][r:]
                    child.append(c)

        for c in child:
            self.chromosomesList.append(c)

    def mutation(self):
        total_gen = self.DNACount * len(self.chromosomesList)
        number_of_mute = int(total_gen * self.mutationRate)
        for n in range(number_of_mute):
            rand = random.randint(0, total_gen - 1)
            self.chromosomesList[int(rand / self.DNACount)][rand % self.DNACount] = random.randint(0, 1000)

    def end(self):
        # fitness = []
        ans = False
        # for c in self.chromosomesList:
        #     fitness.append(self.problem.competency(c))
        #     if self.problem.competency(c) == 0:
        #         self.ans = c
        #         ans = True
        if self.currentGen > self.Maxgeneration:
            ans = True

        # self.bestG.append(min(fitness))
        # self.worstG.append(max(fitness))
        # self.meanG.append(sum(fitness) / len(fitness))
        return ans

    def answer(self):
        print(self.chromosomesList)
        print(self.fitness)
        # print('MIN is :', self.ans)
        # print('ANS is : ', self.ans)

a = problem.Problem()

genetic = Genetic()
genetic.problem = a
genetic.generate_chromosome()
while not genetic.end():
    print('Spin : ', genetic.currentGen)
    genetic.evaluation_fitness()
    genetic.select_chromosome()
    genetic.cross_over()
    genetic.mutation()
genetic.answer()