#include <stdio.h>
#include <conio.h>
#include <locale.h>


struct Plants
{
	char Name[25];
	int Amount;
	char TypeOfPlant[25];
	int NumOfArea;

};

extern void SortByNumOfArea();
extern void AddPlant(int AddValue);
extern void FullDeleteFunction();
extern void DeleteByArea(int Amount);
extern void DeleteByName(char Name[]);
extern void DeleteByType(char Name[]);
extern void DeleteByAmount(int Amount);
extern void DeleteElements(int iElement);
extern void DeleteAllElements();
extern void ReadBioGarden();
extern void OutputPlant();
extern int Menu();
extern void WriteBioGarden();
extern int CheckString(char string[]);
extern int CheckInt(char string[]);
extern void ChangeField();
extern void Sort();
extern void SortByNumOfArea();
extern void SortByName();
extern int EnterINT();





