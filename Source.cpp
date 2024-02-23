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
			printf("������� ����� �������� ������ ��������: ");
			int HowMuchAdd;
			HowMuchAdd = EnterINT();
			if (HowMuchAdd >= 1)
			{
				AddPlant(HowMuchAdd);
			}
			else
			{
				printf("����� �������� ������� 1-� ��������");
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


// 1 ���� � �����(��������� ����� ����� ����������)
// 2 ���� � ����
// 3 ������� ��� ���������� ������� �� �����
// 4 ������� ��� ���������� �������
// 5 �������� N ���-�� ��������
// 6 �����������(������� ������ ����������)
// 7 ������� ��������� ����������