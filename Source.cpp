#include <stdio.h>
#include <conio.h>
#include <locale.h>
#include "Header.h"
#include <Windows.h>

using namespace std;




int main()
{
	SetConsoleCP(1251);
	SetConsoleOutputCP(1251);
	
	int ContinuePeek = 1;
	
	while (ContinuePeek)
	{
		switch (Menu())
		{
		case 1:
			ReadBioGarden();
			
			break;
		case 2:
			WriteBioGarden();
			break;
		case 3:
			OutputPlant();
			break;
		case 4:
			FullDeleteFunction();
			break;
		case 5: 
			printf("Сколько видов растений хотите добавить: ");
			int HowMuchAdd;
			HowMuchAdd = EnterINT();
			if (HowMuchAdd >= 1)
			{
				AddPlant(HowMuchAdd);
			}
			else
			{
				printf("Можно добавить минимум 1-о растение");
			}
			break;
		case 6:
			Sort();
			break;
		case 7:
			ChangeField();
			break;
		case 0:
			ContinuePeek = 0;
		default:
			break;
		}
	}
}


// 1 Ввод с файла(прописать после ввода сортировку)
// 2 Ввод в файл
// 3 Вывести всю статистику биосада на экран
// 4 Удалить всю статистику биосада
// 5 Добавить N кол-во растений
// 6 Сортировать(создать другую сортировку)
// 7 Создать изменение параметров