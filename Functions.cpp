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
	printf("1. ���� � �����\n");
	printf("2. ���� � ����\n");
	printf("3. ������� ��� ���������� ������� �� �����\n");
	printf("4. ������� ��� ���������� �������\n");
	printf("5. �������� N ���-�� ��������\n");
	printf("6. �����������\n");
	printf("7. ��������� ����������\n");
	printf("����: ");
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
	printf("����������� ��: \n");
	printf("1. �����\n");
	printf("2. ������ �������\n");
	int YouCH;
	printf("�����: ");
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
		printf("��� ������ ������");
		break;
	}
}

void SortByNumOfArea()   // ���������� �� �������
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
{ // 1 - ������, ����� - ������
	
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



void AddPlant(int AddValue) // ������� ��������? AddValue
{
	
	for (register int i = SizeArray; i < (SizeArray + AddValue); i++)
	{
		char SameName[25];
		int amount;
		char Type[25];
		int area;
		printf("������� �������� ��������, ������� ����������: ");
		
		gets_s(SameName);
		
		while (!CheckString(SameName))
		{
			printf("�� ���������� ����. \n������� ���������: ");
			gets_s(SameName);
		}
		printf("������� ���������� ������ ��������: ");
		amount = EnterINT();
		
		
		
		printf("������� ��� ������������ ��������: ");
		
		gets_s(Type);
		
		while (!CheckString(Type))
		{
			printf("�� ���������� ����. \n������� ���������: ");
			gets_s(Type);
		}
		printf("������� ����� �������: ");
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
			}if (boolean == 1) // �������� �� ���������� ���
			{
				if (area == PlantsArray[j].NumOfArea) // �������� �� ���������� ������� �������
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
			printf("��� ���� ����� �������� �� ����� �������, � ���: %s", Type);
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
	printf("\t\t��� ������������ �����, ������ �� ���������");
	printf("\n����� ��������� ��� �������� ��:\n");
	printf("1. �� ��������\n");
	printf("2. �� ����������(���� ����� ��������� - ��������� ���)\n");
	printf("3. �� ����(���� ����� ��������� - ��������� ���)\n");
	printf("4. �� ������ �������(���� ����� ��������� - ��������� ���)\n");
	printf("5. ������� ��� ����������\n");
	printf("����: ");
	
	int VoteDelete;
	VoteDelete = EnterINT();
	
	switch (VoteDelete)
	{
	case 1:
		printf("\n---------------------------------------------------\n");
		printf("������� ��������: ");

		char delName[25];
		getchar();
		gets_s(delName);
		while (!CheckString(delName))
		{
			printf("�� ���������� ����. \n������� ���������: ");
			gets_s(delName);
		}
		DeleteByName(delName);
		printf("\n---------------------------------------------------\n");
		break;
	case 2:
		printf("\n---------------------------------------------------\n");
		printf("������� ����������: ");
		char delAmount[25];
		getchar();
		gets_s(delAmount);
		while (!CheckInt(delAmount))
		{
			printf("�� ���������� ����. \n������� ���������: ");
			gets_s(delAmount);
		}
		int delAmount_1;
		delAmount_1 = atoi(delAmount);
		
		DeleteByAmount(delAmount_1);
		printf("\n---------------------------------------------------\n");
		break;
	case 3:
		printf("\n---------------------------------------------------\n");
		printf("������� ���: ");
		char delType[25];
		
		getchar();
		gets_s(delType);
		while (!CheckString(delType))
		{
			printf("�� ���������� ����. \n������� ���������: ");
			gets_s(delType);
		}
		DeleteByType(delType);
		printf("\n---------------------------------------------------\n");
		break;
	case 4:
		printf("\n---------------------------------------------------\n");
		printf("������� ����� �������: ");
		char delArea[25];
		getchar();
		gets_s(delArea);
		while (!CheckInt(delArea))
		{
			printf("�� ���������� ����. \n������� ���������: ");
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
			printf("��� ������ �������� ������!");
			printf("\n---------------------------------------------------\n");
		break;
	}
	
}


void DeleteByName(char Name[]) // ����� �������� ��� ����
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

void DeleteByType(char Name[]) // ����� �������� ��� ����
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
void DeleteByAmount(int Amount) //  ��� �� �������� ��� ������ �������
{
	
	for (register int i = 0; i < SizeArray; i++)
	{
		if (PlantsArray[i].Amount == Amount)
		{
			DeleteElements(i);
		}
	}
}

void DeleteByArea(int Amount) //  ��� �� �������� ��� ������ �������
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
		fscanf_s(file, "���������� ��������: %d\n", &SizeArray);
		PlantsArray = (struct Plants*)realloc(PlantsArray, sizeof(struct Plants) * (SizeArray));
		for (register int i = 0; i < SizeArray; i++)
		{

		
			
			


			fscanf_s(file, "��������: %[^\n]\n", PlantsArray[i].Name, sizeof(PlantsArray[i].Name));
			fscanf_s(file, "����������: %d\n", &PlantsArray[i].Amount);
			fscanf_s(file, "���: %[^\n]\n", PlantsArray[i].TypeOfPlant, sizeof(PlantsArray[i].TypeOfPlant));
			fscanf_s(file, "����� �������: %d", &PlantsArray[i].NumOfArea);


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
			
			printf("��������: %s\n", PlantsArray[i].Name);
			printf("����������: %d\n", PlantsArray[i].Amount);
			printf("���: %s\n", PlantsArray[i].TypeOfPlant);
			printf("����� �������: %d", PlantsArray[i].NumOfArea);
			printf("\n---------------------------------------------------\n");
		}
	}
	else
	{
		printf("\n---------------------------------------------------\n���� �������� � ������������ ���� ������...\n---------------------------------------------------\n");
	}
	
}


void WriteBioGarden()
{

	FILE* file;
	fopen_s(&file, "BioGarden.txt", "w");
	if (file != NULL && PlantsArray != NULL)
	{
		fprintf(file, "���������� ��������: %d\n", SizeArray);
		for (register int i = 0; i < SizeArray; i++)
		{
			
			fprintf(file, "��������: %s\n", PlantsArray[i].Name);
			fprintf(file, "����������: %d\n", PlantsArray[i].Amount);
			fprintf(file, "���: %s\n", PlantsArray[i].TypeOfPlant);
			fprintf(file, "����� �������: %d", PlantsArray[i].NumOfArea);
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
	printf("������� �������� ��������, ���� �������� ������ ��������: ");
	gets_s(NameOfPlant);
	printf("��������: ");
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
			printf("\n%d. ��������: %s", HaveSuchNames, PlantsArray[i].Name);
			printf("\n����������: %d",PlantsArray[i].Amount);
			printf("\n���: %s", PlantsArray[i].TypeOfPlant);
			printf("\n����� �������: %d\n", PlantsArray[i].NumOfArea);
			printf("\n---------------------------------------------------\n");
		}

	}
	if (HaveSuchNames > 0)
	{
		int AskForChoice;
		
		printf("�����: ");
		AskForChoice = EnterINT();
		while (HaveSuchNames < AskForChoice)
		{
			printf("��� ������ ������, ������� ���������: ");
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
					printf("��� ��������: ");
					printf("\n1. ��������");
					printf("\n2. ����������");
					printf("\n3. ���");
					printf("\n4. ����� �������");
					printf("\n�����: ");
					int Choice;
					Choice = EnterINT();
					while (Choice < 0 || Choice > 4) 
					{
						printf("��� ������ ��������, �������� ���������: ");
						Choice = EnterINT();
					}
					
					
						printf("������� ���������: ");
					
					switch (Choice)
					{

					case 1:
						char NameChange[25];
						
						gets_s(NameChange);

						while (!CheckString(NameChange))
						{
							printf("�� ���������� ����. \n������� ���������: ");
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
							printf("�� ���������� ����. \n������� ���������: ");
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

		printf("������\n");
		printf("\n---------------------------------------------------\n");
	}
}


int EnterINT()
{
	char amount[25];
	gets_s(amount);
	while (!CheckInt(amount))
	{
		printf("�� ���������� ����. \n������� ���������: ");
		gets_s(amount);
	}
	int Value;
	Value = atoi(amount);
	return Value;
}


