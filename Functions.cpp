#include <stdlib.h>
#include <stdio.h>
#include <conio.h>
#include <string.h>
#include <locale.h>
#include "Header.h"

int SizeArray = 0;
static Plants* PlantsArray = NULL;
FILE* file;



int Menu()
{
	printf("1. Ввод с файла\n");
	printf("2. Ввод в файл\n");
	printf("3. Вывести всю статистику биосада на экран\n");
	printf("4. Удалить всю статистику биосада\n");
	printf("5. Добавить N кол-во растений\n");
	printf("6. Сортировать\n");
	printf("7. Изменение параметров\n");
	printf("Ввод: ");
	int choice;
	choice = EnterINT();
	printf("\n---------------------------------------------------\n");
	if (choice >= 1 && choice <= 7 )
	{
		return choice;
	}
	else
	{
		return -1;
	}

}

void Sort()
{
	printf("Сортировать по: \n");
	printf("1. Имени\n");
	printf("2. Номеру учатска\n");
	int YouCH;
	printf("Выбор: ");
	scanf_s("%d", &YouCH);
	switch (YouCH)
	{
	case 1:
		SortByName();
		break;
	case 2:
		SortByNumOfArea();
		break;
	default:
		printf("Нет такого выбора");
		break;
	}
}

void SortByNumOfArea()   // сортировка по участку
{
	if (SizeArray >= 2)
	{


		for (register int i = 0; i < SizeArray; i++)
		{
			for (register int j = 0; j < SizeArray - i-1; j++)
			{
				if (PlantsArray[j].NumOfArea > PlantsArray[j + 1].NumOfArea)
				{
					struct Plants jElement = PlantsArray[j];
					PlantsArray[j] = PlantsArray[j + 1];
					PlantsArray[j + 1] = jElement;
				}
			}
		}
	}
}
void SortByName()
{ // 1 - больше, иначе - меньше
	
	if (SizeArray >= 2)
	{
		for (register int i = 0; i < SizeArray; i++)
		{
			for (register int j = 0; j < SizeArray-i-1; j++)
			{
				if (strcmp(PlantsArray[j].Name, PlantsArray[j+1].Name) > 0)
				{
					struct Plants jElement = PlantsArray[j];
					PlantsArray[j] = PlantsArray[j + 1];
					PlantsArray[j + 1] = jElement;
				}
				else if (strcmp(PlantsArray[j].Name, PlantsArray[j + 1].Name) == 0)
				{
					SortByNumOfArea();
				}


			}
		}
	}
}



void AddPlant(int AddValue) // сколько добавить? AddValue
{
	
	for (register int i = SizeArray; i < (SizeArray + AddValue); i++)
	{
		char SameName[25];
		int amount;
		char Type[25];
		int area;
		printf("Введите название растения, которое добавляете: ");
		
		gets_s(SameName);
		
		while (!CheckString(SameName))
		{
			printf("Не правильный ввод. \nВведите правильно: ");
			gets_s(SameName);
		}
		printf("Введите количество данных растений: ");
		amount = EnterINT();
		
		
		
		printf("Введите вид добавляемого растения: ");
		
		gets_s(Type);
		
		while (!CheckString(Type))
		{
			printf("Не правильный ввод. \nВведите правильно: ");
			gets_s(Type);
		}
		printf("Введите номер участка: ");
		area = EnterINT();
		int boolean = 1;
		int boolean_1 = 0;
		for (register int j = 0; j < i; j++)
		{
			boolean = 1;
			for (register int g = 0; g < (sizeof(PlantsArray[i].Name) / sizeof(char)) && boolean == 1; g++)
			{
				if (PlantsArray[j].Name[g] != SameName[g] && boolean != 0)
				{
					boolean = 0;
				}
			}if (boolean == 1) // проверка на совпадение имён
			{
				if (area == PlantsArray[j].NumOfArea) // проверка на совпадение номеров участка
				{
					boolean_1 = 1;
					PlantsArray[j].Amount += amount;
					for (int x = 0; x < sizeof(Type)/sizeof(char); x++)
					{
						Type[x] = PlantsArray[j].TypeOfPlant[x];
					}
				}
				else
				{
					boolean_1 = 0;
				}
			}
			
		}

		if (boolean_1 == 1)
		{
			printf("\n---------------------------------------------------\n");
			printf("Уже есть такое растение на таком участке, её вид: %s", Type);
			printf("\n---------------------------------------------------\n");
			i--;
			AddValue--;
		}
		else
		{
			
			PlantsArray = (Plants*)realloc(PlantsArray, sizeof(Plants) * (SizeArray + 1));
			SizeArray++;
			AddValue--;
			for (int i = 0; i < sizeof(PlantsArray[SizeArray-1].Name) / sizeof(char); i++)
			{
				PlantsArray[SizeArray-1].Name[i] = SameName[i];
			}
			PlantsArray[SizeArray-1].Amount = amount;
			PlantsArray[SizeArray-1].NumOfArea = area;
			for (int i = 0; i < sizeof(PlantsArray[SizeArray-1].Name) / sizeof(char); i++)
			{
				PlantsArray[SizeArray-1].TypeOfPlant[i] = Type[i];
			}
			
		}

		
		
		
	}
}


void FullDeleteFunction()
{
	printf("\t\tПРИ НЕКОРРЕКТНОМ ВВОДЕ, НИЧЕГО НЕ УДАЛИТЬСЯ");
	printf("\nНайти расстение для удаления по:\n");
	printf("1. По названию\n");
	printf("2. По количеству(если таких несколько - удаляться все)\n");
	printf("3. По виду(если таких несколько - удаляться все)\n");
	printf("4. По номеру участка(если таких несколько - удаляться все)\n");
	printf("5. Удалить всю информацию\n");
	printf("Ввод: ");
	
	int VoteDelete;
	VoteDelete = EnterINT();
	
	switch (VoteDelete)
	{
	case 1:
		printf("\n---------------------------------------------------\n");
		printf("Введите название: ");

		char delName[25];
		getchar();
		gets_s(delName);
		while (!CheckString(delName))
		{
			printf("Не правильный ввод. \nВведите правильно: ");
			gets_s(delName);
		}
		DeleteByName(delName);
		printf("\n---------------------------------------------------\n");
		break;
	case 2:
		printf("\n---------------------------------------------------\n");
		printf("Введите количество: ");
		char delAmount[25];
		getchar();
		gets_s(delAmount);
		while (!CheckInt(delAmount))
		{
			printf("Не правильный ввод. \nВведите правильно: ");
			gets_s(delAmount);
		}
		int delAmount_1;
		delAmount_1 = atoi(delAmount);
		
		DeleteByAmount(delAmount_1);
		printf("\n---------------------------------------------------\n");
		break;
	case 3:
		printf("\n---------------------------------------------------\n");
		printf("Введите вид: ");
		char delType[25];
		
		getchar();
		gets_s(delType);
		while (!CheckString(delType))
		{
			printf("Не правильный ввод. \nВведите правильно: ");
			gets_s(delType);
		}
		DeleteByType(delType);
		printf("\n---------------------------------------------------\n");
		break;
	case 4:
		printf("\n---------------------------------------------------\n");
		printf("Введите номер участка: ");
		char delArea[25];
		getchar();
		gets_s(delArea);
		while (!CheckInt(delArea))
		{
			printf("Не правильный ввод. \nВведите правильно: ");
			gets_s(delArea);
		}
		int delArea_1;
		delArea_1 = atoi(delArea);

		DeleteByArea(delArea_1);
		printf("\n---------------------------------------------------\n");
		break;
	case 5: 
		DeleteAllElements();
		printf("\n---------------------------------------------------\n");
		break;
	default:
		getchar();
		printf("\n---------------------------------------------------\n");
			printf("Нет такого варианта выбора!");
			printf("\n---------------------------------------------------\n");
		break;
	}
	
}


void DeleteByName(char Name[]) // также подходит для вида
{

	int boolean;
	for (register int i = 0; i < SizeArray; i++)
	{
		boolean = 1;
		for (register int j = 0; j < sizeof(Name)/sizeof(char) && boolean != 0; j++)
		{
			if (PlantsArray[i].Name[j] != Name[j] && boolean != 0)
			{
				boolean = 0;
				
			}
		}
		if (boolean == 1)
		{
			DeleteElements(i);
		}
			
		
	}
}

void DeleteByType(char Name[]) // также подходит для вида
{

	int boolean;
	for (register int i = 0; i < SizeArray; i++)
	{
		boolean = 1;
		for (register int j = 0; j < sizeof(Name) / sizeof(char) && boolean != 0; j++)
		{
			if (PlantsArray[i].TypeOfPlant[j] != Name[j] && boolean != 0)
			{
				boolean = 0;

			}
		}
		if (boolean == 1)
		{
			DeleteElements(i);
		}


	}
}
void DeleteByAmount(int Amount) //  так же подходит для номера участка
{
	
	for (register int i = 0; i < SizeArray; i++)
	{
		if (PlantsArray[i].Amount == Amount)
		{
			DeleteElements(i);
		}
	}
}

void DeleteByArea(int Amount) //  так же подходит для номера участка
{

	for (register int i = 0; i < SizeArray; i++)
	{
		if (PlantsArray[i].NumOfArea == Amount)
		{
			DeleteElements(i);
		}
	}
}









void DeleteElements(int iElement)
{
	
	for (register int  i = iElement; i < SizeArray-1; i++)
	{
		PlantsArray[i] = PlantsArray[i + 1];
	}
	SizeArray--;
	PlantsArray = (Plants*)realloc(PlantsArray, sizeof(Plants) * SizeArray);
}


void DeleteAllElements()
{
	free(PlantsArray);
	SizeArray = 0;
	
}


void ReadBioGarden() {
	
	fopen_s(&file, "BioGarden.txt", "r");
	PlantsArray = NULL;
	SizeArray = 0;
	if (file != NULL)
	{

		fseek(file, 0, SEEK_SET);
		fscanf_s(file, "Количество растений: %d\n", &SizeArray);
		PlantsArray = (struct Plants*)realloc(PlantsArray, sizeof(struct Plants) * (SizeArray));
		for (register int i = 0; i < SizeArray; i++)
		{

		
			
			


			fscanf_s(file, "Название: %[^\n]\n", PlantsArray[i].Name, sizeof(PlantsArray[i].Name));
			fscanf_s(file, "Количество: %d\n", &PlantsArray[i].Amount);
			fscanf_s(file, "Вид: %[^\n]\n", PlantsArray[i].TypeOfPlant, sizeof(PlantsArray[i].TypeOfPlant));
			fscanf_s(file, "Номер участка: %d", &PlantsArray[i].NumOfArea);


		}
		
		
		

		fclose(file);
	}
	
}



void OutputPlant()
{
	if (SizeArray > 0)
	{
		printf("\n---------------------------------------------------\n");
		for (register int i = 0; i < SizeArray; i++)
		{
			
			printf("Название: %s\n", PlantsArray[i].Name);
			printf("Количество: %d\n", PlantsArray[i].Amount);
			printf("Вид: %s\n", PlantsArray[i].TypeOfPlant);
			printf("Номер участка: %d", PlantsArray[i].NumOfArea);
			printf("\n---------------------------------------------------\n");
		}
	}
	else
	{
		printf("\n---------------------------------------------------\nНету растений в батоническом саду сейчас...\n---------------------------------------------------\n");
	}
	
}


void WriteBioGarden()
{

	FILE* file;
	fopen_s(&file, "BioGarden.txt", "w");
	if (file != NULL && PlantsArray != NULL)
	{
		fprintf(file, "Количество растений: %d\n", SizeArray);
		for (register int i = 0; i < SizeArray; i++)
		{
			
			fprintf(file, "Название: %s\n", PlantsArray[i].Name);
			fprintf(file, "Количество: %d\n", PlantsArray[i].Amount);
			fprintf(file, "Вид: %s\n", PlantsArray[i].TypeOfPlant);
			fprintf(file, "Номер участка: %d", PlantsArray[i].NumOfArea);
		}
		fseek(file, 0, SEEK_SET);
		fclose(file);
	}
	
}



int CheckString(char string[])
{
	


		int boolean = 1;
		for (register int i = 0; i < sizeof(string) / sizeof(char); i++)
		{
			if (string[i] - '0' >= 0 && string[i] - '0' <= 9)
			{
				boolean = 0;
			}
		}
		return boolean;
	
	
}



int CheckInt(char string[])
{
	int boolean = 1;
	for (register int i = 0; string[i] != '\0'; i++)
	{
		if (string[i] < '0' || string[i] > '9')
		{
			boolean = 0;
		}
	}
	return boolean;
}

void ChangeField()
{
	char NameOfPlant[25];
	printf("Введите название растения, поля которого хотите изменить: ");
	gets_s(NameOfPlant);
	printf("Изменить: ");
	int boolean;
	int HaveSuchNames = 0;
	for (register int i = 0; i < SizeArray; i++)
	{
		boolean = 1;
		for (register int j = 0; j < sizeof(PlantsArray[i].Name)/sizeof(char) && boolean != 0; j++)
		{
			if (PlantsArray[i].Name[j] != NameOfPlant[j])
			{
				boolean = 0;
			}
		}
		if (boolean == 1)
		{
			HaveSuchNames++;
			printf("\n%d. Название: %s", HaveSuchNames, PlantsArray[i].Name);
			printf("\nКоличество: %d",PlantsArray[i].Amount);
			printf("\nВид: %s", PlantsArray[i].TypeOfPlant);
			printf("\nНомер участка: %d\n", PlantsArray[i].NumOfArea);
			printf("\n---------------------------------------------------\n");
		}

	}
	if (HaveSuchNames > 0)
	{
		int AskForChoice;
		
		printf("Выбор: ");
		AskForChoice = EnterINT();
		while (HaveSuchNames < AskForChoice)
		{
			printf("Нет такого выбора, введите корректно: ");
			scanf_s("%d", &AskForChoice);
		}
		for (register int i = 0; i < SizeArray; i++)
		{
			boolean = 1;
			for (register int j = 0; j < sizeof(PlantsArray[i].Name) / sizeof(char) && boolean != 0; j++)
			{
				if (PlantsArray[i].Name[j] != NameOfPlant[j])
				{
					boolean = 0;
				}
			}
			if (boolean == 1)
			{
				
				
				AskForChoice--;
				if (AskForChoice == 0)
				{
					printf("\n---------------------------------------------------\n");
					printf("Что изменить: ");
					printf("\n1. Название");
					printf("\n2. Количество");
					printf("\n3. Вид");
					printf("\n4. Номер участка");
					printf("\nВыбор: ");
					int Choice;
					Choice = EnterINT();
					while (Choice < 0 || Choice > 4) 
					{
						printf("Нет такого варианта, напишите корректно: ");
						Choice = EnterINT();
					}
					
					
						printf("Вводите изменения: ");
					
					switch (Choice)
					{

					case 1:
						char NameChange[25];
						
						gets_s(NameChange);

						while (!CheckString(NameChange))
						{
							printf("Не правильный ввод. \nВведите правильно: ");
							gets_s(NameChange);
						}
						for (int j = 0; j < sizeof(PlantsArray[i].Name) / sizeof(char); j++)
						{
							PlantsArray[i].Name[j] = NameChange[j];
						}
						printf("\n---------------------------------------------------\n");
						break;
					case 2:
						int amountChange;
						
						amountChange = EnterINT();
						PlantsArray[i].Amount = amountChange;
						printf("\n---------------------------------------------------\n");
						break;
					case 3:
						char TypeChange[25];
						
						gets_s(TypeChange);

						while (!CheckString(TypeChange))
						{
							printf("Не правильный ввод. \nВведите правильно: ");
							gets_s(TypeChange);
						}
						for (int j = 0; j < sizeof(PlantsArray[i].Name) / sizeof(char); j++)
						{
							PlantsArray[i].TypeOfPlant[j] = TypeChange[j];
						}
						printf("\n---------------------------------------------------\n");
						break;
					case 4:
						int AreaChange;
						
						AreaChange = EnterINT();
						PlantsArray[i].NumOfArea = AreaChange;
						printf("\n---------------------------------------------------\n");
						break;

					
					default:
						break;
						break;
					}
					
				}
			}

		}
	}
	else
	{

		printf("нечего\n");
		printf("\n---------------------------------------------------\n");
	}
}


int EnterINT()
{
	char amount[25];
	gets_s(amount);
	while (!CheckInt(amount))
	{
		printf("Не правильный ввод. \nВведите правильно: ");
		gets_s(amount);
	}
	int Value;
	Value = atoi(amount);
	return Value;
}


