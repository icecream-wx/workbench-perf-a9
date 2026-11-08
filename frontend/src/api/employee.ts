import { get, request } from './http';
import type { PageResult } from './types';

export type EmployeeStatus = 'ACTIVE' | 'INACTIVE';

export interface Employee {
  id: number;
  empNo: string;
  name: string;
  department: string | null;
  position: string | null;
  phone: string | null;
  email: string | null;
  hireDate: string | null;
  status: EmployeeStatus | string;
  createdAt: string;
  updatedAt: string;
}

export interface EmployeePayload {
  empNo: string;
  name: string;
  department: string | null;
  position: string | null;
  phone: string | null;
  email: string | null;
  hireDate: string | null;
  status: EmployeeStatus;
}

export interface EmployeeQuery {
  page: number;
  size: number;
  keyword?: string;
}

export interface EmployeePage extends PageResult<Employee> {
  page: number;
  size: number;
}

export function listEmployees(query: EmployeeQuery): Promise<EmployeePage> {
  return get<EmployeePage>('/employees', { params: query });
}

export function getEmployee(id: number): Promise<Employee> {
  return get<Employee>(`/employees/${id}`);
}

export function createEmployee(payload: EmployeePayload): Promise<Employee> {
  return request<Employee>({ method: 'POST', url: '/employees', data: payload });
}

export function updateEmployee(id: number, payload: EmployeePayload): Promise<Employee> {
  return request<Employee>({ method: 'PUT', url: `/employees/${id}`, data: payload });
}
