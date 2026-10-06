export interface Customer {
  id: number;
  keycloakId: string;

  firstName: string;
  lastName: string;
  email: string;

  phone?: string;

  street?: string;
  houseNumber?: string;
  postalCode?: string;
  city?: string;
  country?: string;

  createdAt?: string;
  updatedAt?: string;
}
